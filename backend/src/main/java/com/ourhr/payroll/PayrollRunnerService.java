package com.ourhr.payroll;

import com.ourhr.auth.User;
import com.ourhr.auth.UserRepository;
import com.ourhr.common.ResourceNotFoundException;
import com.ourhr.common.TenantContext;
import com.ourhr.company.BranchService;
import com.ourhr.company.dto.BranchResponse;
import com.ourhr.employee.Employee;
import com.ourhr.employee.EmployeeRepository;
import com.ourhr.employee.salary.EmployeeSalaryStructure;
import com.ourhr.employee.salary.SalaryStructureService;
import com.ourhr.attendance.MonthlyAttendanceSummary;
import com.ourhr.attendance.MonthlyAttendanceSummaryRepository;
import com.ourhr.payroll.dto.InitiatePayrollRunRequest;
import com.ourhr.payroll.dto.PayrollAuditDiffResponse;
import com.ourhr.payroll.dto.PayrollRunResponse;
import com.ourhr.payroll.dto.PayrollStepTransitionRequest;
import com.ourhr.payroll.dto.PayslipSummaryDto;
import com.ourhr.statutory.PtSlab;
import com.ourhr.statutory.StatutoryQueryService;
import com.ourhr.statutory.TdsTaxSlab;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PayrollRunnerService {

    private final PayrollRunRepository payrollRunRepository;
    private final PayslipLineItemRepository payslipLineItemRepository;
    private final LopReversalRepository lopReversalRepository;
    private final MonthlyAttendanceSummaryRepository monthlyAttendanceSummaryRepository;
    private final EmployeeRepository employeeRepository;
    private final SalaryStructureService salaryStructureService;
    private final StatutoryQueryService statutoryQueryService;
    private final BranchService branchService;
    private final UserRepository userRepository;
    private final PayrollAuditService payrollAuditService;

    @Transactional
    public PayrollRunResponse initiatePayrollRun(InitiatePayrollRunRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant context is required");
        }

        if (payrollRunRepository.existsByCompanyIdAndPayrollMonthAndPayrollYear(
                tenantId, request.getPayrollMonth(), request.getPayrollYear())) {
            throw new IllegalStateException("Payroll run already exists for period: "
                    + request.getPayrollMonth() + "/" + request.getPayrollYear());
        }

        UUID initiatorId = resolveCurrentUserId();
        if (initiatorId == null) {
            initiatorId = findFallbackUserId(tenantId);
        }

        PayrollRun run = PayrollRun.builder()
                .payrollMonth(request.getPayrollMonth())
                .payrollYear(request.getPayrollYear())
                .runStatus("DRAFT")
                .totalGrossPay(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                .totalNetPay(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                .totalPfLiability(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                .totalEsicLiability(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                .totalPtLiability(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                .totalTdsLiability(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                .initiatedBy(initiatorId)
                .build();
        run.setCompanyId(tenantId);

        PayrollRun saved = payrollRunRepository.save(run);
        return toResponse(saved);
    }

    @Transactional
    public PayrollRunResponse lockAttendance(UUID payrollRunId) {
        UUID tenantId = TenantContext.getTenantId();
        PayrollRun run = getRunOrThrow(tenantId, payrollRunId);
        assertNotDisbursed(run);

        List<MonthlyAttendanceSummary> summaries = monthlyAttendanceSummaryRepository
                .findByCompanyIdAndPayrollMonthAndPayrollYear(tenantId, run.getPayrollMonth(), run.getPayrollYear());

        if (summaries.isEmpty()) {
            throw new IllegalStateException("Monthly attendance summary has not been generated for period: "
                    + run.getPayrollMonth() + "/" + run.getPayrollYear());
        }

        boolean allLocked = summaries.stream().allMatch(s -> Boolean.TRUE.equals(s.getIsLocked()));
        if (!allLocked) {
            throw new IllegalStateException("Monthly attendance summary is not locked for period: "
                    + run.getPayrollMonth() + "/" + run.getPayrollYear());
        }

        run.setRunStatus("ATTENDANCE_LOCKED");
        PayrollRun saved = payrollRunRepository.save(run);
        return toResponse(saved);
    }

    @Transactional
    public PayrollRunResponse applyAdjustments(UUID payrollRunId) {
        UUID tenantId = TenantContext.getTenantId();
        PayrollRun run = getRunOrThrow(tenantId, payrollRunId);
        assertNotDisbursed(run);

        List<LopReversal> approvedReversals = lopReversalRepository.findByCompanyIdAndStatus(tenantId, "APPROVED");
        for (LopReversal reversal : approvedReversals) {
            if (reversal.getProcessedInPayrollRunId() == null || reversal.getProcessedInPayrollRunId().equals(payrollRunId)) {
                reversal.setProcessedInPayrollRunId(payrollRunId);
                reversal.setStatus("PROCESSED");
                lopReversalRepository.save(reversal);
            }
        }

        run.setRunStatus("ADJUSTMENTS_APPLIED");
        PayrollRun saved = payrollRunRepository.save(run);
        return toResponse(saved);
    }

    @Transactional
    public PayrollRunResponse calculatePayroll(UUID payrollRunId) {
        UUID tenantId = TenantContext.getTenantId();
        PayrollRun run = getRunOrThrow(tenantId, payrollRunId);
        assertNotDisbursed(run);

        if ("APPROVED".equalsIgnoreCase(run.getRunStatus()) || "DISBURSED".equalsIgnoreCase(run.getRunStatus())) {
            throw new IllegalStateException("Cannot recalculate payroll run in status " + run.getRunStatus());
        }

        // Recalculation law: purge draft line items
        payslipLineItemRepository.deleteByPayrollRunIdAndCompanyId(payrollRunId, tenantId);
        payslipLineItemRepository.flush();

        List<Employee> activeEmployees = employeeRepository.findByCompanyId(tenantId).stream()
                .filter(e -> "ACTIVE".equalsIgnoreCase(e.getEmploymentStatus()))
                .filter(e -> !"CONSULTANT_194J".equalsIgnoreCase(e.getWorkerType()))
                .toList();

        Map<UUID, MonthlyAttendanceSummary> attendanceMap = monthlyAttendanceSummaryRepository
                .findByCompanyIdAndPayrollMonthAndPayrollYear(tenantId, run.getPayrollMonth(), run.getPayrollYear())
                .stream()
                .collect(Collectors.toMap(MonthlyAttendanceSummary::getEmployeeId, Function.identity(), (a, b) -> a));

        Map<UUID, List<LopReversal>> reversalMap = lopReversalRepository
                .findByCompanyIdAndProcessedInPayrollRunId(tenantId, payrollRunId)
                .stream()
                .collect(Collectors.groupingBy(LopReversal::getEmployeeId));

        LocalDate payrollDate = YearMonth.of(run.getPayrollYear(), run.getPayrollMonth()).atEndOfMonth();
        BigDecimal epfCeiling = statutoryQueryService.getEpfWageCeiling(payrollDate);
        BigDecimal esicCeiling = statutoryQueryService.getEsicWageCeiling(payrollDate);

        BigDecimal totalGross = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalNet = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalPf = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalEsic = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalPt = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalTds = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        List<PayslipLineItem> itemsToSave = new ArrayList<>();

        for (Employee emp : activeEmployees) {
            Optional<EmployeeSalaryStructure> structureOpt = salaryStructureService
                    .findActiveStructure(emp.getId(), payrollDate);
            if (structureOpt.isEmpty()) {
                continue;
            }
            EmployeeSalaryStructure structure = structureOpt.get();

            MonthlyAttendanceSummary attendance = attendanceMap.get(emp.getId());
            int totalDays = (attendance != null && attendance.getTotalCalendarDays() > 0)
                    ? attendance.getTotalCalendarDays()
                    : YearMonth.of(run.getPayrollYear(), run.getPayrollMonth()).lengthOfMonth();

            BigDecimal payableDays = (attendance != null && attendance.getPayableDays() != null)
                    ? attendance.getPayableDays()
                    : BigDecimal.valueOf(totalDays);

            BigDecimal prorationRatio = payableDays.divide(BigDecimal.valueOf(totalDays), 6, RoundingMode.HALF_UP);
            if (prorationRatio.compareTo(BigDecimal.ONE) > 0) {
                prorationRatio = BigDecimal.ONE;
            }

            BigDecimal earnedBasic = structure.getBasicPay().multiply(prorationRatio).setScale(2, RoundingMode.HALF_UP);
            BigDecimal earnedDa = structure.getDaPay() != null
                    ? structure.getDaPay().multiply(prorationRatio).setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            BigDecimal earnedHra = structure.getHraPay().multiply(prorationRatio).setScale(2, RoundingMode.HALF_UP);
            BigDecimal earnedSpecial = structure.getSpecialAllowance().multiply(prorationRatio).setScale(2, RoundingMode.HALF_UP);

            List<LopReversal> empReversals = reversalMap.getOrDefault(emp.getId(), List.of());
            BigDecimal reversalAmount = empReversals.stream()
                    .map(LopReversal::getReversalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);

            BigDecimal empGross = earnedBasic.add(earnedDa).add(earnedHra).add(earnedSpecial).add(reversalAmount)
                    .setScale(2, RoundingMode.HALF_UP);

            itemsToSave.add(buildLineItem(tenantId, payrollRunId, emp.getId(), "EARNING", "Basic Pay", earnedBasic));
            if (earnedDa.compareTo(BigDecimal.ZERO) > 0) {
                itemsToSave.add(buildLineItem(tenantId, payrollRunId, emp.getId(), "EARNING", "Dearness Allowance", earnedDa));
            }
            itemsToSave.add(buildLineItem(tenantId, payrollRunId, emp.getId(), "EARNING", "House Rent Allowance", earnedHra));
            itemsToSave.add(buildLineItem(tenantId, payrollRunId, emp.getId(), "EARNING", "Special Allowance", earnedSpecial));
            if (reversalAmount.compareTo(BigDecimal.ZERO) > 0) {
                itemsToSave.add(buildLineItem(tenantId, payrollRunId, emp.getId(), "EARNING", "LOP Reversal Adjustment", reversalAmount));
            }

            BigDecimal empPf = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            BigDecimal employerPf = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            if (!Boolean.TRUE.equals(structure.getPfOptOut())) {
                BigDecimal epfWage = earnedBasic.add(earnedDa);
                BigDecimal cappedWage = epfWage.min(epfCeiling);
                empPf = cappedWage.multiply(new BigDecimal("0.12")).setScale(2, RoundingMode.HALF_UP);
                employerPf = cappedWage.multiply(new BigDecimal("0.12")).setScale(2, RoundingMode.HALF_UP);
                if (empPf.compareTo(BigDecimal.ZERO) > 0) {
                    itemsToSave.add(buildLineItem(tenantId, payrollRunId, emp.getId(), "STATUTORY_DEDUCTION", "Employee PF Contribution", empPf));
                }
            }

            BigDecimal empEsic = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            BigDecimal employerEsic = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            if (empGross.compareTo(esicCeiling) <= 0) {
                empEsic = empGross.multiply(new BigDecimal("0.0075")).setScale(2, RoundingMode.HALF_UP);
                employerEsic = empGross.multiply(new BigDecimal("0.0325")).setScale(2, RoundingMode.HALF_UP);
                if (empEsic.compareTo(BigDecimal.ZERO) > 0) {
                    itemsToSave.add(buildLineItem(tenantId, payrollRunId, emp.getId(), "STATUTORY_DEDUCTION", "Employee ESIC Contribution", empEsic));
                }
            }

            String stateCode = "KA";
            if (emp.getBranchId() != null) {
                try {
                    BranchResponse branch = branchService.getBranchById(emp.getBranchId());
                    if (branch != null && branch.getStateCode() != null) {
                        stateCode = branch.getStateCode();
                    }
                } catch (Exception ignored) {
                }
            }

            BigDecimal empPt = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            Optional<PtSlab> slabOpt = statutoryQueryService.getPtSlab(stateCode, emp.getGender(), empGross, payrollDate);
            if (slabOpt.isPresent()) {
                PtSlab slab = slabOpt.get();
                if (slab.getSpecialMonth() != null
                        && slab.getSpecialMonth().equals(run.getPayrollMonth())
                        && slab.getSpecialMonthTaxAmount() != null) {
                    empPt = slab.getSpecialMonthTaxAmount().setScale(2, RoundingMode.HALF_UP);
                } else if (slab.getTaxAmount() != null) {
                    empPt = slab.getTaxAmount().setScale(2, RoundingMode.HALF_UP);
                }
            }
            if (empPt.compareTo(BigDecimal.ZERO) > 0) {
                itemsToSave.add(buildLineItem(tenantId, payrollRunId, emp.getId(), "STATUTORY_DEDUCTION", "Professional Tax", empPt));
            }

            BigDecimal empTds = estimateTds(empGross, payrollDate);
            if (empTds.compareTo(BigDecimal.ZERO) > 0) {
                itemsToSave.add(buildLineItem(tenantId, payrollRunId, emp.getId(), "STATUTORY_DEDUCTION", "TDS / Income Tax", empTds));
            }

            BigDecimal totalDeductions = empPf.add(empEsic).add(empPt).add(empTds).setScale(2, RoundingMode.HALF_UP);
            BigDecimal netPay = empGross.subtract(totalDeductions).setScale(2, RoundingMode.HALF_UP);

            totalGross = totalGross.add(empGross);
            totalNet = totalNet.add(netPay);
            totalPf = totalPf.add(empPf.add(employerPf));
            totalEsic = totalEsic.add(empEsic.add(employerEsic));
            totalPt = totalPt.add(empPt);
            totalTds = totalTds.add(empTds);
        }

        payslipLineItemRepository.saveAll(itemsToSave);

        run.setRunStatus("CALCULATED");
        run.setTotalGrossPay(totalGross);
        run.setTotalNetPay(totalNet);
        run.setTotalPfLiability(totalPf);
        run.setTotalEsicLiability(totalEsic);
        run.setTotalPtLiability(totalPt);
        run.setTotalTdsLiability(totalTds);

        PayrollRun saved = payrollRunRepository.save(run);
        return toResponse(saved);
    }

    @Transactional
    public List<PayrollAuditDiffResponse> auditPayroll(UUID payrollRunId) {
        UUID tenantId = TenantContext.getTenantId();
        PayrollRun run = getRunOrThrow(tenantId, payrollRunId);
        assertNotDisbursed(run);

        List<PayrollAuditDiffResponse> diffs = payrollAuditService.auditRun(run);

        run.setRunStatus("AUDITED");
        payrollRunRepository.save(run);
        return diffs;
    }

    @Transactional
    public PayrollRunResponse approvePayroll(UUID payrollRunId) {
        UUID tenantId = TenantContext.getTenantId();
        PayrollRun run = getRunOrThrow(tenantId, payrollRunId);
        assertNotDisbursed(run);

        validateApprovalAuthority();

        if (!"AUDITED".equalsIgnoreCase(run.getRunStatus())) {
            throw new IllegalStateException("Cannot approve payroll run in status " + run.getRunStatus() + ". Expected status: AUDITED");
        }

        run.setRunStatus("APPROVED");
        run.setApprovedBy(resolveCurrentUserId());
        run.setApprovedAt(Instant.now());

        PayrollRun saved = payrollRunRepository.save(run);
        return toResponse(saved);
    }

    @Transactional
    public PayrollRunResponse disbursePayroll(UUID payrollRunId) {
        UUID tenantId = TenantContext.getTenantId();
        PayrollRun run = getRunOrThrow(tenantId, payrollRunId);

        if (!"APPROVED".equalsIgnoreCase(run.getRunStatus())) {
            throw new IllegalStateException("Cannot disburse payroll run in status " + run.getRunStatus() + ". Expected status: APPROVED");
        }

        run.setRunStatus("DISBURSED");
        PayrollRun saved = payrollRunRepository.save(run);
        return toResponse(saved);
    }

    @Transactional
    public PayrollRunResponse transitionStep(UUID payrollRunId, PayrollStepTransitionRequest request) {
        String target = request.getTargetStatus() != null ? request.getTargetStatus().trim().toUpperCase() : "";
        return switch (target) {
            case "ATTENDANCE_LOCKED" -> lockAttendance(payrollRunId);
            case "ADJUSTMENTS_APPLIED" -> applyAdjustments(payrollRunId);
            case "CALCULATED" -> calculatePayroll(payrollRunId);
            case "AUDITED" -> {
                auditPayroll(payrollRunId);
                yield getPayrollRun(payrollRunId);
            }
            case "APPROVED" -> approvePayroll(payrollRunId);
            case "DISBURSED" -> disbursePayroll(payrollRunId);
            default -> throw new IllegalArgumentException("Unsupported payroll transition status: " + request.getTargetStatus());
        };
    }

    @Transactional(readOnly = true)
    public PayrollRunResponse getPayrollRun(UUID payrollRunId) {
        UUID tenantId = TenantContext.getTenantId();
        PayrollRun run = getRunOrThrow(tenantId, payrollRunId);
        return toResponse(run);
    }

    @Transactional(readOnly = true)
    public List<PayrollAuditDiffResponse> getPayrollAudit(UUID payrollRunId) {
        UUID tenantId = TenantContext.getTenantId();
        PayrollRun run = getRunOrThrow(tenantId, payrollRunId);
        return payrollAuditService.auditRun(run);
    }

    @Transactional(readOnly = true)
    public PayslipSummaryDto getPayslipSummary(UUID payrollRunId, UUID employeeId) {
        UUID tenantId = TenantContext.getTenantId();
        getRunOrThrow(tenantId, payrollRunId);

        Employee employee = employeeRepository.findByIdAndCompanyId(employeeId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + employeeId));

        List<PayslipLineItem> items = payslipLineItemRepository
                .findByCompanyIdAndPayrollRunIdAndEmployeeId(tenantId, payrollRunId, employeeId);

        BigDecimal earnings = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal deductions = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        List<PayslipSummaryDto.PayslipItemDto> itemDtos = new ArrayList<>();

        for (PayslipLineItem item : items) {
            if ("EARNING".equalsIgnoreCase(item.getItemCategory())) {
                earnings = earnings.add(item.getItemAmount());
            } else if ("STATUTORY_DEDUCTION".equalsIgnoreCase(item.getItemCategory())
                    || "OTHER_DEDUCTION".equalsIgnoreCase(item.getItemCategory())) {
                deductions = deductions.add(item.getItemAmount());
            }

            itemDtos.add(PayslipSummaryDto.PayslipItemDto.builder()
                    .category(item.getItemCategory())
                    .name(item.getItemName())
                    .amount(item.getItemAmount())
                    .build());
        }

        BigDecimal netPay = earnings.subtract(deductions).setScale(2, RoundingMode.HALF_UP);

        return PayslipSummaryDto.builder()
                .employeeId(employee.getId())
                .employeeCode(employee.getEmployeeCode())
                .employeeName(employee.getFirstName() + " " + employee.getLastName())
                .department(employee.getDepartment())
                .designation(employee.getDesignation())
                .grossEarnings(earnings)
                .totalDeductions(deductions)
                .netPay(netPay)
                .lineItems(itemDtos)
                .build();
    }

    public PayrollRunResponse toResponse(PayrollRun run) {
        return PayrollRunResponse.builder()
                .id(run.getId())
                .companyId(run.getCompanyId())
                .payrollMonth(run.getPayrollMonth())
                .payrollYear(run.getPayrollYear())
                .runStatus(run.getRunStatus())
                .totalGrossPay(run.getTotalGrossPay())
                .totalNetPay(run.getTotalNetPay())
                .totalPfLiability(run.getTotalPfLiability())
                .totalEsicLiability(run.getTotalEsicLiability())
                .totalPtLiability(run.getTotalPtLiability())
                .totalTdsLiability(run.getTotalTdsLiability())
                .bankPayoutFileUrl(run.getBankPayoutFileUrl())
                .ecrTxtFileUrl(run.getEcrTxtFileUrl())
                .initiatedBy(run.getInitiatedBy())
                .approvedBy(run.getApprovedBy())
                .approvedAt(run.getApprovedAt())
                .createdAt(run.getCreatedAt())
                .updatedAt(run.getUpdatedAt())
                .build();
    }

    private PayrollRun getRunOrThrow(UUID tenantId, UUID payrollRunId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant context is required");
        }
        return payrollRunRepository.findByCompanyIdAndId(tenantId, payrollRunId)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll run not found with id: " + payrollRunId));
    }

    private void assertNotDisbursed(PayrollRun run) {
        if ("DISBURSED".equalsIgnoreCase(run.getRunStatus())) {
            throw new IllegalStateException("Payroll run is DISBURSED and immutable");
        }
    }

    private PayslipLineItem buildLineItem(
            UUID companyId,
            UUID payrollRunId,
            UUID employeeId,
            String category,
            String name,
            BigDecimal amount
    ) {
        PayslipLineItem item = PayslipLineItem.builder()
                .payrollRunId(payrollRunId)
                .employeeId(employeeId)
                .itemCategory(category)
                .itemName(name)
                .itemAmount(amount)
                .build();
        item.setCompanyId(companyId);
        return item;
    }

    private BigDecimal estimateTds(BigDecimal monthlyGross, LocalDate payrollDate) {
        String financialYear = resolveFinancialYear(payrollDate);
        List<TdsTaxSlab> slabs = statutoryQueryService.getTdsSlabs(financialYear, "NEW");
        if (slabs.isEmpty()) {
            slabs = statutoryQueryService.getTdsSlabs("2026-2027", "NEW");
        }
        if (slabs.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal annualGross = monthlyGross.multiply(BigDecimal.valueOf(12));
        BigDecimal stdDeduction = slabs.get(0).getStandardDeduction() != null
                ? slabs.get(0).getStandardDeduction()
                : new BigDecimal("75000.00");
        BigDecimal taxableIncome = annualGross.subtract(stdDeduction).max(BigDecimal.ZERO);

        BigDecimal rebateLimit = slabs.get(0).getRebate87aLimit() != null
                ? slabs.get(0).getRebate87aLimit()
                : new BigDecimal("1275000.00");

        if (taxableIncome.compareTo(rebateLimit) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal annualTax = BigDecimal.ZERO;
        for (TdsTaxSlab slab : slabs) {
            BigDecimal from = slab.getIncomeFrom();
            BigDecimal to = slab.getIncomeTo();
            BigDecimal rate = slab.getTaxRate();

            if (taxableIncome.compareTo(from) > 0 && rate != null && rate.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal slabUpper = (to != null) ? taxableIncome.min(to) : taxableIncome;
                BigDecimal taxableInSlab = slabUpper.subtract(from).max(BigDecimal.ZERO);
                BigDecimal slabTax = taxableInSlab.multiply(rate.divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP));
                annualTax = annualTax.add(slabTax);
            }
        }

        BigDecimal cess = annualTax.multiply(new BigDecimal("0.04")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalAnnualTax = annualTax.add(cess);

        return totalAnnualTax.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
    }

    private String resolveFinancialYear(LocalDate date) {
        int month = date.getMonthValue();
        int year = date.getYear();
        if (month >= 4) {
            return year + "-" + (year + 1);
        } else {
            return (year - 1) + "-" + year;
        }
    }

    private void validateApprovalAuthority() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getAuthorities() != null && !auth.getAuthorities().isEmpty()) {
            Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
            boolean hasAuthority = authorities.stream().anyMatch(a ->
                    "ROLE_FOUNDER".equals(a.getAuthority()) || "ROLE_FINANCE".equals(a.getAuthority()));
            if (!hasAuthority) {
                throw new AccessDeniedException("Only users with ROLE_FOUNDER or ROLE_FINANCE can approve payroll runs");
            }
        }
    }

    private UUID resolveCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getName() != null) {
            return userRepository.findByEmail(auth.getName().trim().toLowerCase())
                    .map(User::getId)
                    .orElse(null);
        }
        return null;
    }

    private UUID findFallbackUserId(UUID tenantId) {
        return userRepository.findAll().stream()
                .filter(u -> tenantId.equals(u.getCompanyId()))
                .findFirst()
                .map(User::getId)
                .orElse(UUID.randomUUID());
    }
}

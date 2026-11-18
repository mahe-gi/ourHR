package com.ourhr.exporter.ca;

import com.ourhr.common.ResourceNotFoundException;
import com.ourhr.common.TenantContext;
import com.ourhr.company.Branch;
import com.ourhr.company.BranchRepository;
import com.ourhr.employee.Employee;
import com.ourhr.employee.EmployeeRepository;
import com.ourhr.employee.kyc.EmployeeKycVault;
import com.ourhr.employee.kyc.EmployeeKycVaultRepository;
import com.ourhr.exporter.bank.BankExporterService;
import com.ourhr.exporter.ca.dto.CaComplianceSummaryResponse;
import com.ourhr.exporter.ca.dto.CaPtReturnSummaryResponse;
import com.ourhr.exporter.ca.dto.CaTaxAuditSummaryResponse;
import com.ourhr.exporter.ecr.EcrExporterService;
import com.ourhr.payroll.PayrollRun;
import com.ourhr.payroll.PayrollRunRepository;
import com.ourhr.payroll.PayslipLineItem;
import com.ourhr.payroll.PayslipLineItemRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service providing read-only compliance, audit, and return data for external Chartered Accountants (ROLE_CA).
 */
@Service
@RequiredArgsConstructor
public class CaPortalService {

    private static final Logger log = LoggerFactory.getLogger(CaPortalService.class);

    private final PayrollRunRepository payrollRunRepository;
    private final PayslipLineItemRepository payslipLineItemRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeKycVaultRepository employeeKycVaultRepository;
    private final BranchRepository branchRepository;
    private final EcrExporterService ecrExporterService;
    private final BankExporterService bankExporterService;

    @Transactional(readOnly = true)
    public CaComplianceSummaryResponse getComplianceSummary(UUID payrollRunId) {
        return getComplianceSummary(payrollRunId, null);
    }

    @Transactional(readOnly = true)
    public CaComplianceSummaryResponse getComplianceSummary(UUID payrollRunId, String yearMonth) {
        UUID companyId = resolveCurrentCompanyId();
        PayrollRun run = null;
        if (payrollRunId != null) {
            run = getPayrollRunOrThrow(companyId, payrollRunId);
        } else if (yearMonth != null && yearMonth.contains("-")) {
            try {
                String[] parts = yearMonth.trim().split("-");
                int y = Integer.parseInt(parts[0]);
                int m = Integer.parseInt(parts[1]);
                run = payrollRunRepository.findByCompanyIdAndPayrollMonthAndPayrollYear(companyId, m, y).orElse(null);
            } catch (Exception ignored) {
            }
        }

        if (run == null) {
            List<PayrollRun> runs = payrollRunRepository.findByCompanyIdOrderByPayrollYearDescPayrollMonthDesc(companyId);
            if (runs.isEmpty()) {
                return CaComplianceSummaryResponse.builder()
                        .payrollRunId(UUID.randomUUID())
                        .payrollMonth(java.time.LocalDate.now().getMonthValue())
                        .payrollYear(java.time.LocalDate.now().getYear())
                        .totalEmployeesCovered(0)
                        .totalGrossWages(BigDecimal.ZERO)
                        .totalNetDisbursed(BigDecimal.ZERO)
                        .totalPfContribution(BigDecimal.ZERO)
                        .totalEsicContribution(BigDecimal.ZERO)
                        .totalPtContribution(BigDecimal.ZERO)
                        .totalTdsDeducted(BigDecimal.ZERO)
                        .ecrFileStatus("NO_RUNS")
                        .bankPayoutStatus("NO_RUNS")
                        .finalizedAt(java.time.Instant.now())
                        .build();
            }
            run = runs.get(0);
        }
        payrollRunId = run.getId();

        List<PayslipLineItem> items = payslipLineItemRepository
                .findByCompanyIdAndPayrollRunId(companyId, payrollRunId);

        long distinctEmployees = items.stream()
                .map(PayslipLineItem::getEmployeeId)
                .distinct()
                .count();

        String ecrStatus = (run.getEcrTxtFileUrl() != null && !run.getEcrTxtFileUrl().isBlank())
                ? "GENERATED" : "PENDING";
        String bankStatus = (run.getBankPayoutFileUrl() != null && !run.getBankPayoutFileUrl().isBlank())
                ? "GENERATED" : "PENDING";

        return CaComplianceSummaryResponse.builder()
                .payrollRunId(run.getId())
                .payrollMonth(run.getPayrollMonth())
                .payrollYear(run.getPayrollYear())
                .totalEmployeesCovered((int) distinctEmployees)
                .totalGrossWages(run.getTotalGrossPay())
                .totalNetDisbursed(run.getTotalNetPay())
                .totalPfContribution(run.getTotalPfLiability())
                .totalEsicContribution(run.getTotalEsicLiability())
                .totalPtContribution(run.getTotalPtLiability())
                .totalTdsDeducted(run.getTotalTdsLiability())
                .ecrFileStatus(ecrStatus)
                .bankPayoutStatus(bankStatus)
                .finalizedAt(run.getApprovedAt() != null ? run.getApprovedAt() : run.getUpdatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public CaTaxAuditSummaryResponse getTaxAuditSummary(UUID payrollRunId) {
        return getTaxAuditSummary(payrollRunId, null);
    }

    @Transactional(readOnly = true)
    public CaTaxAuditSummaryResponse getTaxAuditSummary(UUID payrollRunId, String yearMonth) {
        UUID companyId = resolveCurrentCompanyId();
        PayrollRun run = null;
        if (payrollRunId != null) {
            run = getPayrollRunOrThrow(companyId, payrollRunId);
        } else if (yearMonth != null && yearMonth.contains("-")) {
            try {
                String[] parts = yearMonth.trim().split("-");
                int y = Integer.parseInt(parts[0]);
                int m = Integer.parseInt(parts[1]);
                run = payrollRunRepository.findByCompanyIdAndPayrollMonthAndPayrollYear(companyId, m, y).orElse(null);
            } catch (Exception ignored) {
            }
        }

        if (run == null) {
            List<PayrollRun> runs = payrollRunRepository.findByCompanyIdOrderByPayrollYearDescPayrollMonthDesc(companyId);
            if (runs.isEmpty()) {
                return CaTaxAuditSummaryResponse.builder()
                        .payrollRunId(UUID.randomUUID())
                        .payrollMonth(java.time.LocalDate.now().getMonthValue())
                        .payrollYear(java.time.LocalDate.now().getYear())
                        .totalTaxDeducted(BigDecimal.ZERO)
                        .totalTdsSection192(BigDecimal.ZERO)
                        .totalTdsSection194J(BigDecimal.ZERO)
                        .totalTdsSection194C(BigDecimal.ZERO)
                        .auditLineItems(java.util.Collections.emptyList())
                        .build();
            }
            run = runs.get(0);
        }
        payrollRunId = run.getId();

        List<PayslipLineItem> items = payslipLineItemRepository
                .findByCompanyIdAndPayrollRunId(companyId, payrollRunId);

        Map<UUID, List<PayslipLineItem>> itemsByEmployee = items.stream()
                .collect(Collectors.groupingBy(PayslipLineItem::getEmployeeId));

        BigDecimal tds192 = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal tds194J = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal tds194C = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        List<CaTaxAuditSummaryResponse.TaxAuditLineItemDto> auditList = new ArrayList<>();

        for (Map.Entry<UUID, List<PayslipLineItem>> entry : itemsByEmployee.entrySet()) {
            UUID employeeId = entry.getKey();
            List<PayslipLineItem> empItems = entry.getValue();

            Optional<Employee> empOpt = employeeRepository.findByIdAndCompanyId(employeeId, companyId);
            if (empOpt.isEmpty()) {
                continue;
            }
            Employee emp = empOpt.get();

            Optional<EmployeeKycVault> vaultOpt =
                    employeeKycVaultRepository.findByCompanyIdAndEmployeeId(companyId, employeeId);

            String workerType = emp.getWorkerType() != null ? emp.getWorkerType().trim().toUpperCase(Locale.ROOT) : "";
            String section;
            if (workerType.contains("194J")) {
                section = "194J";
            } else if (workerType.contains("194C")) {
                section = "194C";
            } else {
                section = "192";
            }

            BigDecimal gross = empItems.stream()
                    .filter(i -> "EARNING".equalsIgnoreCase(i.getItemCategory()))
                    .map(PayslipLineItem::getItemAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);

            BigDecimal empTds = empItems.stream()
                    .filter(i -> "STATUTORY_DEDUCTION".equalsIgnoreCase(i.getItemCategory())
                            && i.getItemName() != null
                            && (i.getItemName().toUpperCase(Locale.ROOT).contains("TDS")
                            || i.getItemName().toUpperCase(Locale.ROOT).contains("TAX")))
                    .map(PayslipLineItem::getItemAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);

            if ("194J".equals(section)) {
                tds194J = tds194J.add(empTds);
            } else if ("194C".equals(section)) {
                tds194C = tds194C.add(empTds);
            } else {
                tds192 = tds192.add(empTds);
            }

            String panNumber = vaultOpt.map(EmployeeKycVault::getPanNumber).orElse("");
            String regime = vaultOpt.map(EmployeeKycVault::getTaxRegimePreference).orElse("NEW");

            String firstName = emp.getFirstName() != null ? emp.getFirstName().trim() : "";
            String lastName = emp.getLastName() != null ? emp.getLastName().trim() : "";
            String memberName = (firstName + (lastName.isBlank() ? "" : " " + lastName)).trim();

            auditList.add(CaTaxAuditSummaryResponse.TaxAuditLineItemDto.builder()
                    .employeeId(emp.getId())
                    .employeeCode(emp.getEmployeeCode())
                    .employeeName(memberName)
                    .panNumber(panNumber)
                    .section(section)
                    .grossAmount(gross)
                    .tdsDeducted(empTds)
                    .regime(regime)
                    .build());
        }

        BigDecimal totalTax = tds192.add(tds194J).add(tds194C).setScale(2, RoundingMode.HALF_UP);

        return CaTaxAuditSummaryResponse.builder()
                .payrollRunId(run.getId())
                .payrollMonth(run.getPayrollMonth())
                .payrollYear(run.getPayrollYear())
                .totalTdsSection192(tds192)
                .totalTdsSection194J(tds194J)
                .totalTdsSection194C(tds194C)
                .totalTaxDeducted(totalTax)
                .auditLineItems(auditList)
                .build();
    }

    @Transactional(readOnly = true)
    public CaPtReturnSummaryResponse getPtReturnSummary(UUID payrollRunId) {
        UUID companyId = resolveCurrentCompanyId();
        PayrollRun run = getPayrollRunOrThrow(companyId, payrollRunId);

        List<PayslipLineItem> items = payslipLineItemRepository
                .findByCompanyIdAndPayrollRunId(companyId, payrollRunId);

        Map<UUID, List<PayslipLineItem>> itemsByEmployee = items.stream()
                .collect(Collectors.groupingBy(PayslipLineItem::getEmployeeId));

        Map<String, StatePtAccumulator> stateMap = new HashMap<>();

        for (Map.Entry<UUID, List<PayslipLineItem>> entry : itemsByEmployee.entrySet()) {
            UUID employeeId = entry.getKey();
            List<PayslipLineItem> empItems = entry.getValue();

            Optional<Employee> empOpt = employeeRepository.findByIdAndCompanyId(employeeId, companyId);
            if (empOpt.isEmpty()) {
                continue;
            }
            Employee emp = empOpt.get();

            String stateCode = "KA";
            if (emp.getBranchId() != null) {
                Optional<Branch> branchOpt = branchRepository.findById(emp.getBranchId());
                if (branchOpt.isPresent() && branchOpt.get().getStateCode() != null) {
                    stateCode = branchOpt.get().getStateCode().trim().toUpperCase(Locale.ROOT);
                }
            }

            BigDecimal gross = empItems.stream()
                    .filter(i -> "EARNING".equalsIgnoreCase(i.getItemCategory()))
                    .map(PayslipLineItem::getItemAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);

            BigDecimal ptAmount = empItems.stream()
                    .filter(i -> "STATUTORY_DEDUCTION".equalsIgnoreCase(i.getItemCategory())
                            && i.getItemName() != null
                            && (i.getItemName().toLowerCase(Locale.ROOT).contains("professional tax")
                            || i.getItemName().equalsIgnoreCase("pt")))
                    .map(PayslipLineItem::getItemAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);

            StatePtAccumulator acc = stateMap.computeIfAbsent(stateCode, k -> new StatePtAccumulator());
            acc.employeeCount++;
            acc.totalSalary = acc.totalSalary.add(gross);
            acc.taxAmount = acc.taxAmount.add(ptAmount);
        }

        BigDecimal totalPt = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        List<CaPtReturnSummaryResponse.StatePtBreakdownDto> breakdownList = new ArrayList<>();

        for (Map.Entry<String, StatePtAccumulator> entry : stateMap.entrySet()) {
            StatePtAccumulator acc = entry.getValue();
            totalPt = totalPt.add(acc.taxAmount);

            breakdownList.add(CaPtReturnSummaryResponse.StatePtBreakdownDto.builder()
                    .stateCode(entry.getKey())
                    .employeeCount(acc.employeeCount)
                    .totalSalary(acc.totalSalary.setScale(2, RoundingMode.HALF_UP))
                    .taxAmount(acc.taxAmount.setScale(2, RoundingMode.HALF_UP))
                    .build());
        }

        return CaPtReturnSummaryResponse.builder()
                .payrollRunId(run.getId())
                .payrollMonth(run.getPayrollMonth())
                .payrollYear(run.getPayrollYear())
                .totalPtPayable(totalPt)
                .stateBreakdowns(breakdownList)
                .build();
    }

    @Transactional(readOnly = true)
    public byte[] downloadEcrFile(UUID payrollRunId) {
        return ecrExporterService.generateEcrBytes(payrollRunId);
    }

    @Transactional(readOnly = true)
    public byte[] downloadBankPayoutFile(UUID payrollRunId, String bankFormat) {
        return bankExporterService.generateBankCsvBytes(payrollRunId, bankFormat);
    }

    private PayrollRun getPayrollRunOrThrow(UUID companyId, UUID payrollRunId) {
        return payrollRunRepository.findByCompanyIdAndId(companyId, payrollRunId)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll run not found with ID: " + payrollRunId));
    }

    private UUID resolveCurrentCompanyId() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant context is required");
        }
        return tenantId;
    }

    private static class StatePtAccumulator {
        int employeeCount = 0;
        BigDecimal totalSalary = BigDecimal.ZERO;
        BigDecimal taxAmount = BigDecimal.ZERO;
    }
}

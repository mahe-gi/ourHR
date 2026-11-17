package com.ourhr.payroll;

import com.ourhr.auth.User;
import com.ourhr.auth.UserRepository;
import com.ourhr.common.ResourceNotFoundException;
import com.ourhr.common.TenantContext;
import com.ourhr.employee.Employee;
import com.ourhr.employee.EmployeeRepository;
import com.ourhr.employee.salary.EmployeeSalaryStructure;
import com.ourhr.employee.salary.SalaryStructureService;
import com.ourhr.payroll.dto.CreateLopReversalRequest;
import com.ourhr.payroll.dto.LopReversalResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LopReversalService {

    private final LopReversalRepository lopReversalRepository;
    private final EmployeeRepository employeeRepository;
    private final SalaryStructureService salaryStructureService;
    private final UserRepository userRepository;

    @Transactional
    public LopReversalResponse createLopReversal(CreateLopReversalRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant context is required");
        }

        Employee employee = employeeRepository.findByIdAndCompanyId(request.getEmployeeId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + request.getEmployeeId()));

        EmployeeSalaryStructure activeStructure = salaryStructureService.findActiveStructure(employee.getId(), LocalDate.now())
                .orElseThrow(() -> new IllegalStateException("Active salary structure not found for employee: " + employee.getId()));

        YearMonth originalPeriod = YearMonth.of(request.getOriginalPayrollYear(), request.getOriginalPayrollMonth());
        int daysInMonth = originalPeriod.lengthOfMonth();

        BigDecimal perDayRate = activeStructure.getMonthlyGross()
                .divide(BigDecimal.valueOf(daysInMonth), 6, RoundingMode.HALF_UP);
        BigDecimal reversalAmount = perDayRate.multiply(request.getReversedDays())
                .setScale(2, RoundingMode.HALF_UP);

        LopReversal reversal = LopReversal.builder()
                .employeeId(employee.getId())
                .originalPayrollMonth(request.getOriginalPayrollMonth())
                .originalPayrollYear(request.getOriginalPayrollYear())
                .reversedDays(request.getReversedDays())
                .reversalAmount(reversalAmount)
                .status("PENDING")
                .build();
        reversal.setCompanyId(tenantId);

        LopReversal saved = lopReversalRepository.save(reversal);
        return toResponse(saved, employee.getFirstName() + " " + employee.getLastName());
    }

    @Transactional
    public LopReversalResponse approveLopReversal(UUID reversalId) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant context is required");
        }

        LopReversal reversal = lopReversalRepository.findByCompanyIdAndId(tenantId, reversalId)
                .orElseThrow(() -> new ResourceNotFoundException("LOP reversal not found with id: " + reversalId));

        if (!"PENDING".equalsIgnoreCase(reversal.getStatus())) {
            throw new IllegalStateException("LOP reversal is already in status: " + reversal.getStatus());
        }

        UUID approverId = resolveCurrentUserId();
        reversal.setStatus("APPROVED");
        reversal.setApprovedBy(approverId);

        LopReversal saved = lopReversalRepository.save(reversal);

        String employeeName = employeeRepository.findByIdAndCompanyId(saved.getEmployeeId(), tenantId)
                .map(e -> e.getFirstName() + " " + e.getLastName())
                .orElse("Employee " + saved.getEmployeeId());

        return toResponse(saved, employeeName);
    }

    @Transactional(readOnly = true)
    public List<LopReversalResponse> getPendingReversals() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant context is required");
        }

        List<LopReversal> pending = lopReversalRepository.findByCompanyIdAndStatus(tenantId, "PENDING");
        return pending.stream()
                .map(rev -> {
                    String employeeName = employeeRepository.findByIdAndCompanyId(rev.getEmployeeId(), tenantId)
                            .map(e -> e.getFirstName() + " " + e.getLastName())
                            .orElse("Employee " + rev.getEmployeeId());
                    return toResponse(rev, employeeName);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<LopReversalResponse> getReversalsByEmployee(UUID employeeId) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant context is required");
        }

        List<LopReversal> list = lopReversalRepository.findByCompanyIdAndEmployeeId(tenantId, employeeId);
        String employeeName = employeeRepository.findByIdAndCompanyId(employeeId, tenantId)
                .map(e -> e.getFirstName() + " " + e.getLastName())
                .orElse("Employee " + employeeId);

        return list.stream()
                .map(rev -> toResponse(rev, employeeName))
                .collect(Collectors.toList());
    }

    private LopReversalResponse toResponse(LopReversal reversal, String employeeName) {
        return LopReversalResponse.builder()
                .id(reversal.getId())
                .companyId(reversal.getCompanyId())
                .employeeId(reversal.getEmployeeId())
                .employeeName(employeeName)
                .originalPayrollMonth(reversal.getOriginalPayrollMonth() != null ? reversal.getOriginalPayrollMonth() : 0)
                .originalPayrollYear(reversal.getOriginalPayrollYear() != null ? reversal.getOriginalPayrollYear() : 0)
                .reversedDays(reversal.getReversedDays())
                .reversalAmount(reversal.getReversalAmount())
                .status(reversal.getStatus())
                .processedInPayrollRunId(reversal.getProcessedInPayrollRunId())
                .approvedBy(reversal.getApprovedBy())
                .createdAt(reversal.getCreatedAt())
                .build();
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
}

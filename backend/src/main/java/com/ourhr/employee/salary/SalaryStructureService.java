package com.ourhr.employee.salary;

import com.ourhr.common.ResourceNotFoundException;
import com.ourhr.common.TenantContext;
import com.ourhr.employee.EmployeeRepository;
import com.ourhr.employee.salary.dto.SalaryOfferRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SalaryStructureService {

    private final EmployeeSalaryStructureRepository salaryStructureRepository;
    private final EmployeeRepository employeeRepository;
    private final BasicWageValidator basicWageValidator;

    @Transactional
    public EmployeeSalaryStructure createSalaryStructure(SalaryOfferRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant context is required");
        }

        if (request.getEmployeeId() == null) {
            throw new IllegalArgumentException("Employee ID is required");
        }

        employeeRepository.findByIdAndCompanyId(request.getEmployeeId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + request.getEmployeeId()));

        basicWageValidator.validateOrThrow(request);

        salaryStructureRepository.findByCompanyIdAndEmployeeIdAndEffectiveToIsNull(tenantId, request.getEmployeeId())
                .ifPresent(previousActive -> {
                    previousActive.setEffectiveTo(request.getEffectiveFrom().minusDays(1));
                    salaryStructureRepository.save(previousActive);
                });

        EmployeeSalaryStructure structure = EmployeeSalaryStructure.builder()
                .employeeId(request.getEmployeeId())
                .annualCtc(request.getAnnualCtc())
                .monthlyGross(request.getMonthlyGross())
                .basicPay(request.getBasicPay())
                .daPay(request.getDaPay() != null ? request.getDaPay() : BigDecimal.ZERO)
                .hraPay(request.getHraPay())
                .specialAllowance(request.getSpecialAllowance())
                .pfOptOut(Boolean.TRUE.equals(request.getPfOptOut()))
                .effectiveFrom(request.getEffectiveFrom())
                .effectiveTo(null)
                .build();
        structure.setCompanyId(tenantId);

        return salaryStructureRepository.save(structure);
    }

    @Transactional(readOnly = true)
    public EmployeeSalaryStructure getActiveSalaryStructure(UUID employeeId) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant context is required");
        }

        employeeRepository.findByIdAndCompanyId(employeeId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + employeeId));

        return salaryStructureRepository.findActiveStructure(tenantId, employeeId, LocalDate.now())
                .orElseThrow(() -> new ResourceNotFoundException("Active salary structure not found for employee: " + employeeId));
    }
}

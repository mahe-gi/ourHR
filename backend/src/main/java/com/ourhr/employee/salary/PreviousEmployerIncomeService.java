package com.ourhr.employee.salary;

import com.ourhr.common.ResourceNotFoundException;
import com.ourhr.common.TenantContext;
import com.ourhr.employee.EmployeeRepository;
import com.ourhr.employee.salary.dto.PreviousEmployerIncomeResponse;
import com.ourhr.employee.salary.dto.SubmitForm12BRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PreviousEmployerIncomeService {

    private final PreviousEmployerIncomeRepository previousEmployerIncomeRepository;
    private final EmployeeRepository employeeRepository;

    @Transactional
    public PreviousEmployerIncomeResponse savePreviousIncome(SubmitForm12BRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant context is required");
        }

        if (request.getEmployeeId() == null) {
            throw new IllegalArgumentException("Employee ID is required");
        }

        employeeRepository.findByIdAndCompanyId(request.getEmployeeId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + request.getEmployeeId()));

        PreviousEmployerIncome income = previousEmployerIncomeRepository
                .findByCompanyIdAndEmployeeId(tenantId, request.getEmployeeId())
                .orElseGet(() -> {
                    PreviousEmployerIncome newIncome = new PreviousEmployerIncome();
                    newIncome.setCompanyId(tenantId);
                    newIncome.setEmployeeId(request.getEmployeeId());
                    return newIncome;
                });

        income.setFinancialYear(request.getFinancialYear());
        income.setGrossSalary(request.getGrossSalary());
        income.setPfDeducted(request.getPfDeducted() != null ? request.getPfDeducted() : BigDecimal.ZERO);
        income.setPtDeducted(request.getPtDeducted() != null ? request.getPtDeducted() : BigDecimal.ZERO);
        income.setTdsDeducted(request.getTdsDeducted() != null ? request.getTdsDeducted() : BigDecimal.ZERO);
        income.setForm12bDocumentUrl(request.getForm12bDocumentUrl());

        PreviousEmployerIncome saved = previousEmployerIncomeRepository.save(income);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PreviousEmployerIncomeResponse getPreviousIncome(UUID employeeId, String financialYear) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant context is required");
        }

        employeeRepository.findByIdAndCompanyId(employeeId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + employeeId));

        PreviousEmployerIncome income = previousEmployerIncomeRepository
                .findByCompanyIdAndEmployeeIdAndFinancialYear(tenantId, employeeId, financialYear)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Form 12B record not found for employee: " + employeeId + " in FY: " + financialYear));

        return toResponse(income);
    }

    public PreviousEmployerIncomeResponse toResponse(PreviousEmployerIncome income) {
        return PreviousEmployerIncomeResponse.builder()
                .id(income.getId())
                .companyId(income.getCompanyId())
                .employeeId(income.getEmployeeId())
                .financialYear(income.getFinancialYear())
                .grossSalary(income.getGrossSalary())
                .pfDeducted(income.getPfDeducted())
                .ptDeducted(income.getPtDeducted())
                .tdsDeducted(income.getTdsDeducted())
                .form12bDocumentUrl(income.getForm12bDocumentUrl())
                .isVerified(Boolean.TRUE.equals(income.getIsVerified()))
                .createdAt(income.getCreatedAt())
                .build();
    }
}

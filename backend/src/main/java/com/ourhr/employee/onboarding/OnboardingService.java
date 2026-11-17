package com.ourhr.employee.onboarding;

import com.ourhr.common.ResourceNotFoundException;
import com.ourhr.common.TenantContext;
import com.ourhr.employee.Employee;
import com.ourhr.employee.EmployeeRepository;
import com.ourhr.employee.EmployeeService;
import com.ourhr.employee.kyc.EmployeeKycVault;
import com.ourhr.employee.kyc.EmployeeKycVaultRepository;
import com.ourhr.employee.kyc.KycVaultService;
import com.ourhr.employee.onboarding.dto.OnboardingLinkResponse;
import com.ourhr.employee.onboarding.dto.OnboardingStatusResponse;
import com.ourhr.employee.onboarding.dto.OnboardingSubmissionRequest;
import com.ourhr.employee.salary.PreviousEmployerIncomeRepository;
import com.ourhr.employee.salary.PreviousEmployerIncomeService;
import com.ourhr.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OnboardingService {

    private final JwtService jwtService;
    private final EmployeeRepository employeeRepository;
    private final EmployeeService employeeService;
    private final KycVaultService kycVaultService;
    private final PreviousEmployerIncomeService previousEmployerIncomeService;
    private final EmployeeKycVaultRepository kycVaultRepository;
    private final PreviousEmployerIncomeRepository previousEmployerIncomeRepository;

    @Transactional(readOnly = true)
    public OnboardingLinkResponse generateOnboardingLink(UUID employeeId, int expiresInDays) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant context is required");
        }

        Employee employee = employeeRepository.findByIdAndCompanyId(employeeId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + employeeId));

        int days = expiresInDays > 0 ? expiresInDays : 7;
        long expirationMs = (long) days * 24 * 60 * 60 * 1000L;
        Instant expiresAt = Instant.now().plusMillis(expirationMs);

        String token = jwtService.generateOnboardingToken(employee.getId(), tenantId, expirationMs);
        String onboardingUrl = "/onboarding?token=" + token;

        return OnboardingLinkResponse.builder()
                .employeeId(employee.getId())
                .token(token)
                .onboardingUrl(onboardingUrl)
                .expiresAt(expiresAt)
                .build();
    }

    @Transactional
    public OnboardingStatusResponse submitOnboarding(OnboardingSubmissionRequest request) {
        if (request == null || request.getToken() == null || request.getToken().isBlank()) {
            throw new IllegalArgumentException("Onboarding invitation token is required");
        }

        if (!jwtService.isTokenValid(request.getToken())) {
            throw new IllegalArgumentException("Invalid or expired onboarding token");
        }

        UUID companyId = jwtService.extractCompanyId(request.getToken());
        UUID employeeId = jwtService.extractEmployeeId(request.getToken());

        if (companyId == null || employeeId == null) {
            throw new IllegalArgumentException("Malformed onboarding token");
        }

        UUID previousTenantId = TenantContext.getTenantId();
        TenantContext.setTenantId(companyId);
        try {
            Employee employee = employeeRepository.findByIdAndCompanyId(employeeId, companyId)
                    .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + employeeId));

            if (request.getPersonalDetails() != null) {
                employeeService.updateEmployee(employeeId, request.getPersonalDetails());
            }

            if (request.getKycDetails() != null) {
                request.getKycDetails().setEmployeeId(employeeId);
                kycVaultService.saveKycVault(employeeId, request.getKycDetails());
            }

            boolean form12bSubmitted = false;
            if (request.getPreviousEmployerDetails() != null && request.getPreviousEmployerDetails().getGrossSalary() != null) {
                request.getPreviousEmployerDetails().setEmployeeId(employeeId);
                previousEmployerIncomeService.savePreviousIncome(request.getPreviousEmployerDetails());
                form12bSubmitted = true;
            }

            Employee updated = employeeRepository.findByIdAndCompanyId(employeeId, companyId).orElse(employee);

            return OnboardingStatusResponse.builder()
                    .employeeId(employeeId)
                    .employeeCode(updated.getEmployeeCode())
                    .employeeName(updated.getFirstName() + " " + updated.getLastName())
                    .onboardingStatus("COMPLETED")
                    .kycSubmitted(true)
                    .form12bSubmitted(form12bSubmitted)
                    .completedAt(Instant.now())
                    .build();
        } finally {
            if (previousTenantId != null) {
                TenantContext.setTenantId(previousTenantId);
            } else {
                TenantContext.clear();
            }
        }
    }

    @Transactional(readOnly = true)
    public OnboardingStatusResponse getOnboardingStatus(UUID employeeId) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant context is required");
        }

        Employee employee = employeeRepository.findByIdAndCompanyId(employeeId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + employeeId));

        var kycOpt = kycVaultRepository.findByCompanyIdAndEmployeeId(tenantId, employeeId);
        boolean kycSubmitted = kycOpt.isPresent();
        boolean form12bSubmitted = previousEmployerIncomeRepository.findByCompanyIdAndEmployeeId(tenantId, employeeId).isPresent();

        String status = kycSubmitted ? "COMPLETED" : "PENDING";
        Instant completedAt = kycOpt.map(EmployeeKycVault::getUpdatedAt).orElse(null);

        return OnboardingStatusResponse.builder()
                .employeeId(employeeId)
                .employeeCode(employee.getEmployeeCode())
                .employeeName(employee.getFirstName() + " " + employee.getLastName())
                .onboardingStatus(status)
                .kycSubmitted(kycSubmitted)
                .form12bSubmitted(form12bSubmitted)
                .completedAt(completedAt)
                .build();
    }
}

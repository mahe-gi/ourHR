package com.ourhr.employee.kyc;

import com.ourhr.common.ResourceNotFoundException;
import com.ourhr.common.TenantContext;
import com.ourhr.employee.EmployeeRepository;
import com.ourhr.employee.kyc.dto.CreateKycVaultRequest;
import com.ourhr.employee.kyc.dto.KycVaultResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class KycVaultService {

    private static final String MASKED_AADHAAR_TOKEN = "[Aadhaar Redacted]";

    private final EmployeeKycVaultRepository kycVaultRepository;
    private final EmployeeRepository employeeRepository;

    @Transactional
    public KycVaultResponse saveKycVault(UUID employeeId, CreateKycVaultRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant context is required");
        }

        employeeRepository.findByIdAndCompanyId(employeeId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + employeeId));

        EmployeeKycVault kycVault = kycVaultRepository.findByCompanyIdAndEmployeeId(tenantId, employeeId)
                .orElseGet(() -> {
                    EmployeeKycVault newVault = new EmployeeKycVault();
                    newVault.setCompanyId(tenantId);
                    newVault.setEmployeeId(employeeId);
                    return newVault;
                });

        kycVault.setPanNumber(request.getPanNumber().toUpperCase().trim());
        kycVault.setMaskedAadhaarToken(MASKED_AADHAAR_TOKEN);
        kycVault.setUan(request.getUan() != null && !request.getUan().isBlank() ? request.getUan().trim() : null);
        kycVault.setEsicIpNumber(request.getEsicIpNumber() != null && !request.getEsicIpNumber().isBlank() ? request.getEsicIpNumber().trim() : null);
        kycVault.setBankAccountNumber(request.getBankAccountNumber().trim());
        kycVault.setBankIfsc(request.getBankIfsc().toUpperCase().trim());
        kycVault.setBankName(request.getBankName().trim());
        kycVault.setPanDocumentUrl(request.getPanDocumentUrl());
        kycVault.setAadhaarDocumentUrl(request.getAadhaarDocumentUrl());
        kycVault.setChequeDocumentUrl(request.getChequeDocumentUrl());
        kycVault.setAppointmentLetterUrl(request.getAppointmentLetterUrl());

        if (request.getTaxRegimePreference() != null && !request.getTaxRegimePreference().isBlank()) {
            kycVault.setTaxRegimePreference(request.getTaxRegimePreference().toUpperCase().trim());
        }

        EmployeeKycVault saved = kycVaultRepository.save(kycVault);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public KycVaultResponse getKycVault(UUID employeeId) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant context is required");
        }

        EmployeeKycVault kycVault = kycVaultRepository.findByCompanyIdAndEmployeeId(tenantId, employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("KYC vault not found for employee: " + employeeId));

        return toResponse(kycVault);
    }

    public KycVaultResponse toResponse(EmployeeKycVault vault) {
        return KycVaultResponse.builder()
                .id(vault.getId())
                .companyId(vault.getCompanyId())
                .employeeId(vault.getEmployeeId())
                .panNumber(vault.getPanNumber())
                .maskedAadhaarToken(vault.getMaskedAadhaarToken())
                .uan(vault.getUan())
                .esicIpNumber(vault.getEsicIpNumber())
                .bankAccountNumberMasked(maskBankAccount(vault.getBankAccountNumber()))
                .bankIfsc(vault.getBankIfsc())
                .bankName(vault.getBankName())
                .panDocumentUrl(vault.getPanDocumentUrl())
                .aadhaarDocumentUrl(vault.getAadhaarDocumentUrl())
                .chequeDocumentUrl(vault.getChequeDocumentUrl())
                .appointmentLetterUrl(vault.getAppointmentLetterUrl())
                .taxRegimePreference(vault.getTaxRegimePreference())
                .createdAt(vault.getCreatedAt())
                .updatedAt(vault.getUpdatedAt())
                .build();
    }

    public static String maskBankAccount(String accountNumber) {
        if (accountNumber == null || accountNumber.isBlank()) {
            return "";
        }
        String trimmed = accountNumber.trim();
        if (trimmed.length() <= 4) {
            return "******" + trimmed;
        }
        return "******" + trimmed.substring(trimmed.length() - 4);
    }
}

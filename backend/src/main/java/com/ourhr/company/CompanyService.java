package com.ourhr.company;

import com.ourhr.common.ResourceNotFoundException;
import com.ourhr.common.TenantContext;
import com.ourhr.company.dto.CompanyResponse;
import com.ourhr.company.dto.UpdateCompanyRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;

    @Transactional(readOnly = true)
    public CompanyResponse getCurrentCompany() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant context is required");
        }

        Company company = companyRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found for current tenant"));

        return toResponse(company);
    }

    @Transactional(readOnly = true)
    public CompanyResponse getCompanyById(UUID companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found with id: " + companyId));

        return toResponse(company);
    }

    @Transactional
    public CompanyResponse updateCurrentCompany(UpdateCompanyRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant context is required");
        }

        Company company = companyRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found for current tenant"));

        if (request.getTradeName() != null) {
            company.setTradeName(request.getTradeName());
        }
        if (request.getGstin() != null) {
            company.setGstin(request.getGstin());
        }
        if (request.getPfEstablishmentCode() != null) {
            company.setPfEstablishmentCode(request.getPfEstablishmentCode());
        }
        if (request.getEsicCode() != null) {
            company.setEsicCode(request.getEsicCode());
        }
        if (request.getLin() != null) {
            company.setLin(request.getLin());
        }
        if (request.getSignatoryName() != null) {
            if (request.getSignatoryName().isBlank()) {
                throw new IllegalArgumentException("Signatory name cannot be blank");
            }
            company.setSignatoryName(request.getSignatoryName());
        }
        if (request.getSignatoryDesignation() != null) {
            if (request.getSignatoryDesignation().isBlank()) {
                throw new IllegalArgumentException("Signatory designation cannot be blank");
            }
            company.setSignatoryDesignation(request.getSignatoryDesignation());
        }
        if (request.getSignatorySignatureUrl() != null) {
            company.setSignatorySignatureUrl(request.getSignatorySignatureUrl());
        }

        Company updated = companyRepository.save(company);
        return toResponse(updated);
    }

    public CompanyResponse toResponse(Company company) {
        return CompanyResponse.builder()
                .id(company.getId())
                .legalName(company.getLegalName())
                .tradeName(company.getTradeName())
                .panNumber(company.getPanNumber())
                .tanNumber(company.getTanNumber())
                .gstin(company.getGstin())
                .pfEstablishmentCode(company.getPfEstablishmentCode())
                .esicCode(company.getEsicCode())
                .lin(company.getLin())
                .signatoryName(company.getSignatoryName())
                .signatoryDesignation(company.getSignatoryDesignation())
                .signatorySignatureUrl(company.getSignatorySignatureUrl())
                .build();
    }
}

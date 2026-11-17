package com.ourhr.company;

import com.ourhr.common.ResourceNotFoundException;
import com.ourhr.common.TenantContext;
import com.ourhr.company.dto.BranchResponse;
import com.ourhr.company.dto.CreateBranchRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BranchService {

    private final BranchRepository branchRepository;

    @Transactional
    public BranchResponse createBranch(CreateBranchRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant context is required");
        }

        boolean isHq = Boolean.TRUE.equals(request.getIsHeadquarters());

        if (isHq) {
            branchRepository.findByCompanyIdAndIsHeadquartersTrue(tenantId)
                    .ifPresent(existingHq -> {
                        existingHq.setIsHeadquarters(false);
                        branchRepository.save(existingHq);
                    });
        }

        Branch branch = Branch.builder()
                .branchName(request.getBranchName())
                .stateCode(request.getStateCode().toUpperCase())
                .city(request.getCity())
                .addressLine(request.getAddressLine())
                .pincode(request.getPincode())
                .isHeadquarters(isHq)
                .build();
        branch.setCompanyId(tenantId);

        Branch saved = branchRepository.save(branch);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<BranchResponse> getAllBranches() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant context is required");
        }

        return branchRepository.findByCompanyId(tenantId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BranchResponse getBranchById(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant context is required");
        }

        return branchRepository.findByIdAndCompanyId(id, tenantId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public List<BranchResponse> getBranchesByState(String stateCode) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant context is required");
        }

        return branchRepository.findByCompanyIdAndStateCode(tenantId, stateCode.toUpperCase())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public BranchResponse updateBranch(UUID id, CreateBranchRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant context is required");
        }

        Branch branch = branchRepository.findByIdAndCompanyId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found with id: " + id));

        boolean newIsHq = Boolean.TRUE.equals(request.getIsHeadquarters());

        if (newIsHq && !Boolean.TRUE.equals(branch.getIsHeadquarters())) {
            branchRepository.findByCompanyIdAndIsHeadquartersTrue(tenantId)
                    .filter(existing -> !existing.getId().equals(id))
                    .ifPresent(existingHq -> {
                        existingHq.setIsHeadquarters(false);
                        branchRepository.save(existingHq);
                    });
        }

        branch.setBranchName(request.getBranchName());
        branch.setStateCode(request.getStateCode().toUpperCase());
        branch.setCity(request.getCity());
        branch.setAddressLine(request.getAddressLine());
        branch.setPincode(request.getPincode());
        branch.setIsHeadquarters(newIsHq);

        Branch updated = branchRepository.save(branch);
        return toResponse(updated);
    }

    public BranchResponse toResponse(Branch branch) {
        return BranchResponse.builder()
                .id(branch.getId())
                .companyId(branch.getCompanyId())
                .branchName(branch.getBranchName())
                .stateCode(branch.getStateCode())
                .city(branch.getCity())
                .addressLine(branch.getAddressLine())
                .pincode(branch.getPincode())
                .isHeadquarters(branch.getIsHeadquarters())
                .createdAt(branch.getCreatedAt())
                .build();
    }
}

package com.ourhr.company;

import com.ourhr.common.ApiResponse;
import com.ourhr.company.dto.BranchResponse;
import com.ourhr.company.dto.CompanyResponse;
import com.ourhr.company.dto.UpdateCompanyRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/companies")
@RequiredArgsConstructor
@Tag(name = "Company Management", description = "Tenant company profile and configuration endpoints")
public class CompanyController {

    private final CompanyService companyService;
    private final BranchService branchService;

    @GetMapping("/current")
    @PreAuthorize("hasAnyRole('FOUNDER', 'HR', 'FINANCE', 'CA')")
    @Operation(summary = "Retrieve current tenant company profile",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<CompanyResponse>> getCurrentCompany() {
        CompanyResponse response = companyService.getCurrentCompany();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/branches")
    @PreAuthorize("hasAnyRole('FOUNDER', 'HR', 'FINANCE', 'CA', 'EMPLOYEE')")
    @Operation(summary = "Retrieve company branch locations",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<List<BranchResponse>>> getCompanyBranches() {
        List<BranchResponse> responses = branchService.getAllBranches();
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @PutMapping("/current")
    @PreAuthorize("hasAnyRole('FOUNDER', 'HR')")
    @Operation(summary = "Update current tenant company settings",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<CompanyResponse>> updateCurrentCompany(
            @Valid @RequestBody UpdateCompanyRequest request
    ) {
        CompanyResponse response = companyService.updateCurrentCompany(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

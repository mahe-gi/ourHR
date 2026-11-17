package com.ourhr.employee.kyc;

import com.ourhr.common.ApiResponse;
import com.ourhr.employee.kyc.dto.CreateKycVaultRequest;
import com.ourhr.employee.kyc.dto.KycVaultResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
@Tag(name = "KYC Vault Management", description = "Employee KYC vault and sensitive ID redaction endpoints")
public class KycVaultController {

    private final KycVaultService kycVaultService;

    @PostMapping("/{employeeId}/kyc")
    @PreAuthorize("hasAnyRole('FOUNDER', 'HR', 'EMPLOYEE')")
    @Operation(summary = "Save or update employee KYC details", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<KycVaultResponse>> saveKycVault(
            @PathVariable UUID employeeId,
            @Valid @RequestBody CreateKycVaultRequest request
    ) {
        request.setEmployeeId(employeeId);
        KycVaultResponse response = kycVaultService.saveKycVault(employeeId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{employeeId}/kyc")
    @PreAuthorize("hasAnyRole('FOUNDER', 'HR', 'FINANCE', 'CA', 'EMPLOYEE')")
    @Operation(summary = "Get employee KYC details with sensitive fields redacted", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<KycVaultResponse>> getKycVault(
            @PathVariable UUID employeeId
    ) {
        KycVaultResponse response = kycVaultService.getKycVault(employeeId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

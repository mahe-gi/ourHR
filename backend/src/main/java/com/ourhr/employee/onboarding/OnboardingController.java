package com.ourhr.employee.onboarding;

import com.ourhr.common.ApiResponse;
import com.ourhr.employee.onboarding.dto.GenerateOnboardingLinkRequest;
import com.ourhr.employee.onboarding.dto.OnboardingLinkResponse;
import com.ourhr.employee.onboarding.dto.OnboardingStatusResponse;
import com.ourhr.employee.onboarding.dto.OnboardingSubmissionRequest;
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
@RequestMapping("/api/v1/onboarding")
@RequiredArgsConstructor
@Tag(name = "Paperless Onboarding", description = "Self-service candidate onboarding and magic link endpoints")
public class OnboardingController {

    private final OnboardingService onboardingService;

    @PostMapping("/generate-link")
    @PreAuthorize("hasAnyRole('FOUNDER', 'HR')")
    @Operation(summary = "Generate paperless onboarding magic link", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<OnboardingLinkResponse>> generateOnboardingLink(
            @Valid @RequestBody GenerateOnboardingLinkRequest request
    ) {
        OnboardingLinkResponse response = onboardingService.generateOnboardingLink(
                request.getEmployeeId(),
                request.getExpiresInDays()
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/submit")
    @Operation(summary = "Submit paperless onboarding details via magic link token")
    public ResponseEntity<ApiResponse<OnboardingStatusResponse>> submitOnboarding(
            @Valid @RequestBody OnboardingSubmissionRequest request
    ) {
        OnboardingStatusResponse response = onboardingService.submitOnboarding(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/status/{employeeId}")
    @PreAuthorize("hasAnyRole('FOUNDER', 'HR')")
    @Operation(summary = "Get employee onboarding status", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<OnboardingStatusResponse>> getOnboardingStatus(
            @PathVariable UUID employeeId
    ) {
        OnboardingStatusResponse response = onboardingService.getOnboardingStatus(employeeId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

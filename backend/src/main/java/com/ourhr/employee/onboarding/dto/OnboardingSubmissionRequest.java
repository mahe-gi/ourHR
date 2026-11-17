package com.ourhr.employee.onboarding.dto;

import com.ourhr.employee.dto.UpdateEmployeeRequest;
import com.ourhr.employee.kyc.dto.CreateKycVaultRequest;
import com.ourhr.employee.salary.dto.SubmitForm12BRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingSubmissionRequest {

    @NotBlank(message = "Onboarding invitation token is required")
    private String token;

    @Valid
    @NotNull(message = "Personal details are required")
    private UpdateEmployeeRequest personalDetails;

    @Valid
    @NotNull(message = "KYC details are required")
    private CreateKycVaultRequest kycDetails;

    @Valid
    private SubmitForm12BRequest previousEmployerDetails;
}

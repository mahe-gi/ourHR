package com.ourhr.employee.onboarding.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateOnboardingLinkRequest {

    @NotNull(message = "Employee ID is required")
    private UUID employeeId;

    @Builder.Default
    private int expiresInDays = 7;
}

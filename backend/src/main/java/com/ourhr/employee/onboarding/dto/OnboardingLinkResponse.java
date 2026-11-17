package com.ourhr.employee.onboarding.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingLinkResponse {

    private UUID employeeId;
    private String token;
    private String onboardingUrl;
    private Instant expiresAt;
}

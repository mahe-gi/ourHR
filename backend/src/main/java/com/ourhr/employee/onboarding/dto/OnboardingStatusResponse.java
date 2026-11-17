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
public class OnboardingStatusResponse {

    private UUID employeeId;
    private String employeeCode;
    private String employeeName;
    private String onboardingStatus;
    private boolean kycSubmitted;
    private boolean form12bSubmitted;
    private Instant completedAt;
}

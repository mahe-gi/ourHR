package com.ourhr.payroll.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollStepTransitionRequest {

    @NotBlank(message = "Target status is required")
    private String targetStatus;

    private String recalculationReason;
}

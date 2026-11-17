package com.ourhr.leave.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateLeaveTypeRequest {

    @NotBlank(message = "Leave name is required")
    @Size(max = 100, message = "Leave name cannot exceed 100 characters")
    private String leaveName;

    @NotBlank(message = "Leave code is required")
    @Size(max = 20, message = "Leave code cannot exceed 20 characters")
    private String leaveCode;

    @NotNull(message = "Annual quota is required")
    @DecimalMin(value = "0.0", message = "Annual quota cannot be negative")
    private BigDecimal annualQuota;

    @NotBlank(message = "Accrual frequency is required")
    @Pattern(regexp = "^(MONTHLY|QUARTERLY|ANNUAL)$", message = "Accrual frequency must be MONTHLY, QUARTERLY, or ANNUAL")
    private String accrualFrequency;

    @Builder.Default
    private boolean isCarryForwardAllowed = true;

    @Builder.Default
    private BigDecimal maxCarryForwardLimit = new BigDecimal("30.0");

    @Builder.Default
    private boolean isSandwichRuleEnabled = false;
}

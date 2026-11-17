package com.ourhr.payroll.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsultantPayoutRequest {

    @NotNull(message = "Consultant employee ID is required")
    private UUID consultantEmployeeId;

    @NotNull(message = "Gross payout amount is required")
    @DecimalMin(value = "1.00", message = "Gross payout amount must be greater than zero")
    private BigDecimal grossAmount;

    @NotBlank(message = "TDS section is required")
    @Pattern(regexp = "^(194J|194C)$", message = "TDS section must be 194J (Professional fees) or 194C (Contractor)")
    private String tdsSection;

    @NotNull(message = "TDS deduction percentage rate is required")
    @DecimalMin(value = "0.0", message = "TDS rate cannot be negative")
    private BigDecimal tdsRate;

    private String description;
}

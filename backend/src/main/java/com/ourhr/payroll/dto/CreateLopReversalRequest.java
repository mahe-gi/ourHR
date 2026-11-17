package com.ourhr.payroll.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
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
public class CreateLopReversalRequest {

    @NotNull(message = "Employee ID is required")
    private UUID employeeId;

    @NotNull(message = "Original payroll month is required")
    @Min(value = 1, message = "Month must be between 1 and 12")
    @Max(value = 12, message = "Month must be between 1 and 12")
    private Integer originalPayrollMonth;

    @NotNull(message = "Original payroll year is required")
    @Min(value = 2020, message = "Year must be valid")
    private Integer originalPayrollYear;

    @NotNull(message = "Reversed days is required")
    @DecimalMin(value = "0.5", message = "Reversed days must be at least 0.5")
    private BigDecimal reversedDays;

    private String reason;
}

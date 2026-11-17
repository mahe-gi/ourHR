package com.ourhr.employee.salary.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalaryOfferRequest {

    private UUID employeeId;

    @NotNull(message = "Annual CTC is required")
    @DecimalMin(value = "0.01", message = "Annual CTC must be greater than zero")
    private BigDecimal annualCtc;

    @NotNull(message = "Monthly gross is required")
    @DecimalMin(value = "0.01", message = "Monthly gross must be greater than zero")
    private BigDecimal monthlyGross;

    @NotNull(message = "Basic pay is required")
    @DecimalMin(value = "0.01", message = "Basic pay must be greater than zero")
    private BigDecimal basicPay;

    @Builder.Default
    private BigDecimal daPay = BigDecimal.ZERO;

    @NotNull(message = "HRA pay is required")
    @DecimalMin(value = "0.00", message = "HRA pay cannot be negative")
    private BigDecimal hraPay;

    @NotNull(message = "Special allowance is required")
    @DecimalMin(value = "0.00", message = "Special allowance cannot be negative")
    private BigDecimal specialAllowance;

    @Builder.Default
    private Boolean pfOptOut = false;

    @NotNull(message = "Effective date is required")
    private LocalDate effectiveFrom;
}

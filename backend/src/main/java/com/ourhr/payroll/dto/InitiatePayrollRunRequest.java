package com.ourhr.payroll.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InitiatePayrollRunRequest {

    @NotNull(message = "Payroll month is required")
    @Min(value = 1, message = "Payroll month must be between 1 and 12")
    @Max(value = 12, message = "Payroll month must be between 1 and 12")
    private Integer payrollMonth;

    @NotNull(message = "Payroll year is required")
    @Min(value = 2020, message = "Payroll year must be valid")
    private Integer payrollYear;
}

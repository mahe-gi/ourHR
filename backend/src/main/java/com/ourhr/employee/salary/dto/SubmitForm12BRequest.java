package com.ourhr.employee.salary.dto;

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
public class SubmitForm12BRequest {

    private UUID employeeId;

    @NotBlank(message = "Financial year is required")
    @Pattern(regexp = "^[0-9]{4}-[0-9]{4}$", message = "Financial year must follow format YYYY-YYYY e.g. 2026-2027")
    private String financialYear;

    @NotNull(message = "Gross salary from previous employer is required")
    @DecimalMin(value = "0.00", message = "Gross salary cannot be negative")
    private BigDecimal grossSalary;

    @NotNull(message = "PF deducted by previous employer is required")
    @DecimalMin(value = "0.00", message = "PF deducted cannot be negative")
    private BigDecimal pfDeducted;

    @NotNull(message = "PT deducted by previous employer is required")
    @DecimalMin(value = "0.00", message = "PT deducted cannot be negative")
    private BigDecimal ptDeducted;

    @NotNull(message = "TDS deducted by previous employer is required")
    @DecimalMin(value = "0.00", message = "TDS deducted cannot be negative")
    private BigDecimal tdsDeducted;

    private String form12bDocumentUrl;
}

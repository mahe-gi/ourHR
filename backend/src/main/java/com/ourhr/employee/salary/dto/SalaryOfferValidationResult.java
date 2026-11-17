package com.ourhr.employee.salary.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalaryOfferValidationResult {

    private boolean valid;
    private BigDecimal basicPlusDa;
    private BigDecimal monthlyGross;
    private BigDecimal wageRatio;
    private BigDecimal minRequiredRatio;
    private BigDecimal statutoryDeficit;
    private String message;
}

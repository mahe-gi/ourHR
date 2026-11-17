package com.ourhr.statutory.engine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TdsCalculationResult {

    private String financialYear;
    private String regime;
    private BigDecimal projectedAnnualGross;
    private BigDecimal standardDeduction;
    private BigDecimal taxableIncome;
    private BigDecimal taxBeforeCess;
    private BigDecimal section87aRebate;
    private BigDecimal taxAfterRebate;
    private BigDecimal educationCess;
    private BigDecimal totalAnnualTax;
    private BigDecimal taxPaidSoFar;
    private int remainingMonths;
    private BigDecimal monthlyTdsDeduction;
}

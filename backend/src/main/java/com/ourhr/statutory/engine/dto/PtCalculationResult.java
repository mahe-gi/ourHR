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
public class PtCalculationResult {

    private String stateCode;
    private BigDecimal salaryAmount;
    private BigDecimal taxAmount;
    private boolean isSpecialMonth;
}

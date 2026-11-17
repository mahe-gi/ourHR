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
public class LwfCalculationResult {

    private String stateCode;
    private boolean applicable;
    private BigDecimal employeeContribution;
    private BigDecimal employerContribution;
    private BigDecimal totalContribution;
}

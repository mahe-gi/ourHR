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
public class EsicCalculationResult {

    private boolean eligible;
    private BigDecimal wageBase;
    private BigDecimal employeeContribution;
    private BigDecimal employerContribution;
    private BigDecimal totalLiability;
    private String note;
}

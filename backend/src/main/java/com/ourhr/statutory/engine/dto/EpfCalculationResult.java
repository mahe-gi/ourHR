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
public class EpfCalculationResult {

    private BigDecimal wageBase;
    private BigDecimal employeeContribution;
    private BigDecimal employerEpsContribution;
    private BigDecimal employerEpfContribution;
    private BigDecimal edliContribution;
    private BigDecimal adminCharges;
    private BigDecimal totalEmployerLiability;
}

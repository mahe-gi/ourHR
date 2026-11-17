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
public class StatutoryCalculationResult {

    private EpfCalculationResult epf;
    private EsicCalculationResult esic;
    private PtCalculationResult pt;
    private LwfCalculationResult lwf;
    private TdsCalculationResult tds;
    private BigDecimal totalEmployeeDeductions;
    private BigDecimal totalEmployerContributions;
}

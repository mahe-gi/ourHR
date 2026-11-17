package com.ourhr.statutory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LwfRuleDto {

    private UUID id;
    private String stateCode;
    private BigDecimal employeeContribution;
    private BigDecimal employerContribution;
    private String deductionFrequency;
    private List<Integer> deductionMonths;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
}

package com.ourhr.statutory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PtSlabDto {

    private UUID id;
    private String stateCode;
    private String gender;
    private BigDecimal salaryFrom;
    private BigDecimal salaryTo;
    private BigDecimal taxAmount;
    private Integer specialMonth;
    private BigDecimal specialMonthTaxAmount;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
}

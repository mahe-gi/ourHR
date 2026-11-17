package com.ourhr.statutory.dto;

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
public class TdsTaxSlabDto {

    private UUID id;
    private String financialYear;
    private String regime;
    private BigDecimal incomeFrom;
    private BigDecimal incomeTo;
    private BigDecimal taxRate;
    private BigDecimal standardDeduction;
    private BigDecimal rebate87aLimit;
}

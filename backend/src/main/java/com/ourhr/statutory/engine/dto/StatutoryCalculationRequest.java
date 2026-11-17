package com.ourhr.statutory.engine.dto;

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
public class StatutoryCalculationRequest {

    private UUID employeeId;
    private UUID companyId;
    private String branchStateCode;
    private String gender;
    private BigDecimal earnedGross;
    private BigDecimal basicPay;
    @Builder.Default
    private BigDecimal daPay = BigDecimal.ZERO;
    private int payrollMonth;
    private int payrollYear;
    private String financialYear;
    @Builder.Default
    private String taxRegime = "NEW";
    @Builder.Default
    private boolean pfOptOut = false;
    private BigDecimal initialGrossInWindow;
    @Builder.Default
    private boolean isPwd = false;
    @Builder.Default
    private BigDecimal previousEmployerGross = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal previousEmployerTds = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal ytdEarnedGross = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal ytdTdsDeducted = BigDecimal.ZERO;
}

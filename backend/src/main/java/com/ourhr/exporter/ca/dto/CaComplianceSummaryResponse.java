package com.ourhr.exporter.ca.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaComplianceSummaryResponse {

    private UUID payrollRunId;
    private int payrollMonth;
    private int payrollYear;
    private int totalEmployeesCovered;
    private BigDecimal totalGrossWages;
    private BigDecimal totalNetDisbursed;
    private BigDecimal totalPfContribution;
    private BigDecimal totalEsicContribution;
    private BigDecimal totalPtContribution;
    private BigDecimal totalTdsDeducted;
    private String ecrFileStatus;
    private String bankPayoutStatus;
    private Instant finalizedAt;
}

package com.ourhr.payroll.dto;

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
public class PayrollRunResponse {

    private UUID id;
    private UUID companyId;
    private int payrollMonth;
    private int payrollYear;
    private String runStatus;
    private BigDecimal totalGrossPay;
    private BigDecimal totalNetPay;
    private BigDecimal totalPfLiability;
    private BigDecimal totalEsicLiability;
    private BigDecimal totalPtLiability;
    private BigDecimal totalTdsLiability;
    private String bankPayoutFileUrl;
    private String ecrTxtFileUrl;
    private UUID initiatedBy;
    private UUID approvedBy;
    private Instant approvedAt;
    private Instant createdAt;
    private Instant updatedAt;
}

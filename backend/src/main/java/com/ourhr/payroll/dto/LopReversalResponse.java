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
public class LopReversalResponse {

    private UUID id;
    private UUID companyId;
    private UUID employeeId;
    private String employeeName;
    private int originalPayrollMonth;
    private int originalPayrollYear;
    private BigDecimal reversedDays;
    private BigDecimal reversalAmount;
    private String status;
    private UUID processedInPayrollRunId;
    private UUID approvedBy;
    private Instant createdAt;
}

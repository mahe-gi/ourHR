package com.ourhr.payroll.dto;

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
public class PayrollAuditDiffResponse {

    private UUID employeeId;
    private String employeeCode;
    private String employeeName;
    private BigDecimal previousNetPay;
    private BigDecimal currentNetPay;
    private BigDecimal netPayDifference;
    private String auditFlagReason;
}

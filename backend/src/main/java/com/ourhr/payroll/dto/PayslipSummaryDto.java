package com.ourhr.payroll.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayslipSummaryDto {

    private UUID employeeId;
    private String employeeCode;
    private String employeeName;
    private String department;
    private String designation;
    private BigDecimal grossEarnings;
    private BigDecimal totalDeductions;
    private BigDecimal netPay;
    private List<PayslipItemDto> lineItems;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PayslipItemDto {
        private String category;
        private String name;
        private BigDecimal amount;
    }
}

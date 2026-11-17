package com.ourhr.exporter.ca.dto;

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
public class CaTaxAuditSummaryResponse {

    private UUID payrollRunId;
    private int payrollMonth;
    private int payrollYear;
    private BigDecimal totalTdsSection192;
    private BigDecimal totalTdsSection194J;
    private BigDecimal totalTdsSection194C;
    private BigDecimal totalTaxDeducted;
    private List<TaxAuditLineItemDto> auditLineItems;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaxAuditLineItemDto {
        private UUID employeeId;
        private String employeeCode;
        private String employeeName;
        private String panNumber;
        private String section;
        private BigDecimal grossAmount;
        private BigDecimal tdsDeducted;
        private String regime;
    }
}

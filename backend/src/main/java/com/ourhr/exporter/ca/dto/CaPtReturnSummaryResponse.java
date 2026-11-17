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
public class CaPtReturnSummaryResponse {

    private UUID payrollRunId;
    private int payrollMonth;
    private int payrollYear;
    private BigDecimal totalPtPayable;
    private List<StatePtBreakdownDto> stateBreakdowns;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatePtBreakdownDto {
        private String stateCode;
        private int employeeCount;
        private BigDecimal totalSalary;
        private BigDecimal taxAmount;
    }
}

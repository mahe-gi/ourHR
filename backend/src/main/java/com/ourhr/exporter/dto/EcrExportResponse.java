package com.ourhr.exporter.dto;

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
public class EcrExportResponse {

    private UUID payrollRunId;
    private String establishmentCode;
    private int payrollMonth;
    private int payrollYear;
    private String fileName;
    private String fileDownloadUrl;
    private int memberCount;
    private BigDecimal totalGrossWages;
    private BigDecimal totalEpfWages;
    private BigDecimal totalEeShare;
    private BigDecimal totalEpsShare;
    private BigDecimal totalErShare;
    private Instant generatedAt;
}

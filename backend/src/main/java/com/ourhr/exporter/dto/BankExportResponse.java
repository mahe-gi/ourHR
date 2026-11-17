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
public class BankExportResponse {

    private UUID payrollRunId;
    private String bankFormat;
    private String fileName;
    private String fileDownloadUrl;
    private int totalRecords;
    private BigDecimal totalAmount;
    private Instant generatedAt;
}

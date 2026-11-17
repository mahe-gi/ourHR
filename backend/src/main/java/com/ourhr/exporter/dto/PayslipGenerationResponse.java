package com.ourhr.exporter.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayslipGenerationResponse {

    private UUID batchJobId;
    private UUID payrollRunId;
    private String status;
    private int totalPayslipsScheduled;
    private Instant scheduledAt;
}

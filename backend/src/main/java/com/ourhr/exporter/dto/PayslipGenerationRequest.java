package com.ourhr.exporter.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayslipGenerationRequest {

    @NotNull(message = "Payroll run ID is required")
    private UUID payrollRunId;

    private UUID employeeId;
}

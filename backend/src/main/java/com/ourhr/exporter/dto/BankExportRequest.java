package com.ourhr.exporter.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankExportRequest {

    @NotNull(message = "Payroll run ID is required")
    private UUID payrollRunId;

    @NotBlank(message = "Bank format is required")
    @Pattern(regexp = "^(HDFC|ICICI|SBI|AXIS)$", message = "Bank format must be HDFC, ICICI, SBI, or AXIS")
    private String bankFormat;
}

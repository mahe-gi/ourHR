package com.ourhr.exporter;

import com.ourhr.common.ApiResponse;
import com.ourhr.exporter.bank.BankExporterService;
import com.ourhr.exporter.dto.BankExportRequest;
import com.ourhr.exporter.dto.BankExportResponse;
import com.ourhr.exporter.dto.EcrExportResponse;
import com.ourhr.exporter.dto.PayslipGenerationRequest;
import com.ourhr.exporter.dto.PayslipGenerationResponse;
import com.ourhr.exporter.ecr.EcrExporterService;
import com.ourhr.exporter.pdf.PdfPayslipService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;
import java.util.UUID;

/**
 * REST controller for executing statutory compliance exports, bank payout file generation,
 * and asynchronous PDF payslip rendering.
 */
@RestController
@RequestMapping("/api/v1/exporters")
@RequiredArgsConstructor
@Tag(name = "Compliance Exporters", description = "Generates EPFO ECR .txt, Bank payout CSVs, and PDF payslips")
public class ExporterController {

    private final EcrExporterService ecrExporterService;
    private final BankExporterService bankExporterService;
    private final PdfPayslipService pdfPayslipService;

    @PostMapping("/ecr/{payrollRunId}")
    @PreAuthorize("hasAnyRole('FOUNDER', 'HR', 'FINANCE', 'CA')")
    @Operation(summary = "Generate and upload official EPFO ECR 2.0 text file for payroll run")
    public ApiResponse<EcrExportResponse> generateEcr(@PathVariable UUID payrollRunId) {
        EcrExportResponse response = ecrExporterService.generateEcrFile(payrollRunId);
        return ApiResponse.success(response);
    }

    @PostMapping("/bank")
    @PreAuthorize("hasAnyRole('FOUNDER', 'HR', 'FINANCE', 'CA')")
    @Operation(summary = "Generate and upload Bank NEFT/RTGS CSV payout file (HDFC, ICICI, SBI, AXIS)")
    public ApiResponse<BankExportResponse> exportBankPayout(@Valid @RequestBody BankExportRequest request) {
        BankExportResponse response = bankExporterService.exportBankPayout(request);
        return ApiResponse.success(response);
    }

    @PostMapping("/payslips")
    @PreAuthorize("hasAnyRole('FOUNDER', 'HR', 'FINANCE', 'CA')")
    @Operation(summary = "Schedule asynchronous batch job for PDF payslip generation")
    public ApiResponse<PayslipGenerationResponse> generatePayslips(@Valid @RequestBody PayslipGenerationRequest request) {
        PayslipGenerationResponse response;
        if (request.getEmployeeId() != null) {
            String url = pdfPayslipService.renderAndStorePayslip(request.getPayrollRunId(), request.getEmployeeId());
            response = PayslipGenerationResponse.builder()
                    .batchJobId(UUID.randomUUID())
                    .payrollRunId(request.getPayrollRunId())
                    .status("COMPLETED")
                    .totalPayslipsScheduled(1)
                    .scheduledAt(java.time.Instant.now())
                    .build();
        } else {
            response = pdfPayslipService.generatePayslipsAsync(request.getPayrollRunId());
        }
        return ApiResponse.success(response);
    }

    @GetMapping("/payslips/{payrollRunId}/{employeeId}")
    @PreAuthorize("hasAnyRole('FOUNDER', 'HR', 'FINANCE', 'CA', 'EMPLOYEE')")
    @Operation(summary = "Download single employee PDF payslip stream")
    public ResponseEntity<byte[]> downloadPayslip(
            @PathVariable UUID payrollRunId,
            @PathVariable UUID employeeId) {

        byte[] pdfBytes = pdfPayslipService.generatePayslipPdf(payrollRunId, employeeId);
        String fileName = String.format(Locale.ROOT, "payslip_%s_%s.pdf", payrollRunId, employeeId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}

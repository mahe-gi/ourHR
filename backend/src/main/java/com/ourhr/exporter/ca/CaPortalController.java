package com.ourhr.exporter.ca;

import com.ourhr.common.ApiResponse;
import com.ourhr.exporter.ca.dto.CaComplianceSummaryResponse;
import com.ourhr.exporter.ca.dto.CaPtReturnSummaryResponse;
import com.ourhr.exporter.ca.dto.CaTaxAuditSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;
import java.util.UUID;

/**
 * REST controller for external Chartered Accountant (CA) compliance portal.
 * Read-only access restricted to CA, FOUNDER, and FINANCE roles.
 */
@RestController
@RequestMapping("/api/v1/ca")
@RequiredArgsConstructor
@Tag(name = "External CA Portal", description = "Read-only statutory compliance workspace for external Chartered Accountants")
public class CaPortalController {

    private final CaPortalService caPortalService;

    @GetMapping("/compliance-summary/{payrollRunId}")
    @PreAuthorize("hasAnyRole('CA', 'FOUNDER', 'FINANCE')")
    @Operation(summary = "Get monthly statutory compliance summary for a payroll run")
    public ApiResponse<CaComplianceSummaryResponse> getComplianceSummary(@PathVariable UUID payrollRunId) {
        CaComplianceSummaryResponse summary = caPortalService.getComplianceSummary(payrollRunId);
        return ApiResponse.success(summary);
    }

    @GetMapping("/tax-audit/{payrollRunId}")
    @PreAuthorize("hasAnyRole('CA', 'FOUNDER', 'FINANCE')")
    @Operation(summary = "Get Form 24Q tax audit breakdown for Section 192, 194J, and 194C")
    public ApiResponse<CaTaxAuditSummaryResponse> getTaxAuditSummary(@PathVariable UUID payrollRunId) {
        CaTaxAuditSummaryResponse taxAudit = caPortalService.getTaxAuditSummary(payrollRunId);
        return ApiResponse.success(taxAudit);
    }

    @GetMapping("/pt-returns/{payrollRunId}")
    @PreAuthorize("hasAnyRole('CA', 'FOUNDER', 'FINANCE')")
    @Operation(summary = "Get state-wise Professional Tax return summary and employee counts")
    public ApiResponse<CaPtReturnSummaryResponse> getPtReturnSummary(@PathVariable UUID payrollRunId) {
        CaPtReturnSummaryResponse ptReturns = caPortalService.getPtReturnSummary(payrollRunId);
        return ApiResponse.success(ptReturns);
    }

    @GetMapping("/ecr/{payrollRunId}")
    @PreAuthorize("hasAnyRole('CA', 'FOUNDER', 'FINANCE')")
    @Operation(summary = "Download EPFO ECR 2.0 text file byte stream")
    public ResponseEntity<byte[]> downloadEcrFile(@PathVariable UUID payrollRunId) {
        byte[] fileBytes = caPortalService.downloadEcrFile(payrollRunId);
        String fileName = String.format(Locale.ROOT, "ECR_%s.txt", payrollRunId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(fileBytes);
    }

    @GetMapping("/bank-file/{payrollRunId}")
    @PreAuthorize("hasAnyRole('CA', 'FOUNDER', 'FINANCE')")
    @Operation(summary = "Download Bank NEFT/RTGS CSV payout file byte stream")
    public ResponseEntity<byte[]> downloadBankPayoutFile(
            @PathVariable UUID payrollRunId,
            @RequestParam(defaultValue = "HDFC") String bankFormat) {

        byte[] fileBytes = caPortalService.downloadBankPayoutFile(payrollRunId, bankFormat);
        String fileName = String.format(Locale.ROOT, "bank_payout_%s_%s.csv",
                bankFormat.toLowerCase(Locale.ROOT), payrollRunId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .header(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8")
                .body(fileBytes);
    }
}

package com.ourhr.payroll;

import com.ourhr.common.ApiResponse;
import com.ourhr.payroll.dto.InitiatePayrollRunRequest;
import com.ourhr.payroll.dto.PayrollAuditDiffResponse;
import com.ourhr.payroll.dto.PayrollRunResponse;
import com.ourhr.payroll.dto.PayrollStepTransitionRequest;
import com.ourhr.payroll.dto.PayslipSummaryDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payroll")
@RequiredArgsConstructor
public class PayrollController {

    private final PayrollRunnerService payrollRunnerService;

    @PostMapping("/initiate")
    @PreAuthorize("hasAnyRole('FOUNDER', 'HR', 'FINANCE')")
    public ResponseEntity<ApiResponse<PayrollRunResponse>> initiatePayrollRun(
            @Valid @RequestBody InitiatePayrollRunRequest request
    ) {
        PayrollRunResponse response = payrollRunnerService.initiatePayrollRun(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PostMapping("/{id}/step")
    @PreAuthorize("hasAnyRole('FOUNDER', 'HR', 'FINANCE')")
    public ResponseEntity<ApiResponse<PayrollRunResponse>> transitionStep(
            @PathVariable("id") UUID id,
            @Valid @RequestBody PayrollStepTransitionRequest request
    ) {
        PayrollRunResponse response = payrollRunnerService.transitionStep(id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/calculate")
    @PreAuthorize("hasAnyRole('FOUNDER', 'HR', 'FINANCE')")
    public ResponseEntity<ApiResponse<PayrollRunResponse>> calculatePayroll(
            @PathVariable("id") UUID id
    ) {
        PayrollRunResponse response = payrollRunnerService.calculatePayroll(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('FOUNDER', 'HR', 'FINANCE', 'CA')")
    public ResponseEntity<ApiResponse<PayrollRunResponse>> getPayrollRun(
            @PathVariable("id") UUID id
    ) {
        PayrollRunResponse response = payrollRunnerService.getPayrollRun(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}/audit")
    @PreAuthorize("hasAnyRole('FOUNDER', 'HR', 'FINANCE', 'CA')")
    public ResponseEntity<ApiResponse<List<PayrollAuditDiffResponse>>> getPayrollAudit(
            @PathVariable("id") UUID id
    ) {
        List<PayrollAuditDiffResponse> response = payrollRunnerService.getPayrollAudit(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}/payslips/{employeeId}")
    @PreAuthorize("hasAnyRole('FOUNDER', 'HR', 'FINANCE', 'CA', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<PayslipSummaryDto>> getPayslipSummary(
            @PathVariable("id") UUID id,
            @PathVariable("employeeId") UUID employeeId
    ) {
        PayslipSummaryDto response = payrollRunnerService.getPayslipSummary(id, employeeId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

package com.ourhr.leave;

import com.ourhr.common.ApiResponse;
import com.ourhr.leave.dto.ApplyLeaveRequest;
import com.ourhr.leave.dto.CreateLeaveTypeRequest;
import com.ourhr.leave.dto.CreditLeaveRequest;
import com.ourhr.leave.dto.LeaveApplicationResponse;
import com.ourhr.leave.dto.LeaveBalanceResponse;
import com.ourhr.leave.dto.LeaveTypeResponse;
import com.ourhr.leave.dto.RejectLeaveRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/leaves")
@RequiredArgsConstructor
@Tag(name = "Leave Management", description = "Endpoints for leave types, application workflow with Indian Sandwich rule, and immutable ledger balances")
public class LeaveController {

    private final LeaveLedgerService leaveLedgerService;

    @PostMapping("/types")
    @PreAuthorize("hasAnyRole('FOUNDER', 'HR')")
    @Operation(summary = "Create a leave type configuration", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<LeaveTypeResponse>> createLeaveType(
            @Valid @RequestBody CreateLeaveTypeRequest request
    ) {
        LeaveTypeResponse response = leaveLedgerService.createLeaveType(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping("/types")
    @PreAuthorize("hasAnyRole('FOUNDER', 'HR', 'EMPLOYEE')")
    @Operation(summary = "List all configured leave types", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<List<LeaveTypeResponse>>> getAllLeaveTypes() {
        List<LeaveTypeResponse> responses = leaveLedgerService.getAllLeaveTypes();
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @PostMapping("/apply")
    @PreAuthorize("hasAnyRole('FOUNDER', 'HR', 'EMPLOYEE')")
    @Operation(summary = "Apply for leave with automatic Sandwich rule verification", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<LeaveApplicationResponse>> applyLeave(
            @Valid @RequestBody ApplyLeaveRequest request
    ) {
        LeaveApplicationResponse response = leaveLedgerService.applyLeave(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('FOUNDER', 'HR')")
    @Operation(summary = "Approve a leave application and record consumption in ledger", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<LeaveApplicationResponse>> approveLeave(@PathVariable UUID id) {
        LeaveApplicationResponse response = leaveLedgerService.approveLeave(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('FOUNDER', 'HR')")
    @Operation(summary = "Reject a leave application", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<LeaveApplicationResponse>> rejectLeave(
            @PathVariable UUID id,
            @RequestBody(required = false) RejectLeaveRequest request,
            @RequestParam(value = "reason", required = false) String reasonParam
    ) {
        String reason = "Rejected by reviewer";
        if (request != null && request.getReason() != null && !request.getReason().isBlank()) {
            reason = request.getReason();
        } else if (reasonParam != null && !reasonParam.isBlank()) {
            reason = reasonParam;
        }

        LeaveApplicationResponse response = leaveLedgerService.rejectLeave(id, reason);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/balances/{employeeId}")
    @PreAuthorize("hasAnyRole('FOUNDER', 'HR', 'EMPLOYEE')")
    @Operation(summary = "Get immutable leave ledger balances for an employee", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<List<LeaveBalanceResponse>>> getLeaveBalances(@PathVariable UUID employeeId) {
        List<LeaveBalanceResponse> responses = leaveLedgerService.getLeaveBalances(employeeId);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @PostMapping("/credit")
    @PreAuthorize("hasAnyRole('FOUNDER', 'HR')")
    @Operation(summary = "Credit leave days to an employee (Accrual transaction)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<LeaveBalanceResponse>> creditLeave(
            @Valid @RequestBody CreditLeaveRequest request
    ) {
        LeaveBalanceResponse response = leaveLedgerService.creditLeave(
                request.getEmployeeId(),
                request.getLeaveTypeId(),
                request.getDays(),
                request.getNote()
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/applications")
    @PreAuthorize("hasAnyRole('FOUNDER', 'HR', 'EMPLOYEE')")
    @Operation(summary = "List leave applications", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<List<LeaveApplicationResponse>>> getLeaveApplications(
            @RequestParam(value = "employeeId", required = false) UUID employeeId,
            @RequestParam(value = "status", required = false) String status
    ) {
        List<LeaveApplicationResponse> responses = leaveLedgerService.getLeaveApplications(employeeId, status);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/applications/{id}")
    @PreAuthorize("hasAnyRole('FOUNDER', 'HR', 'EMPLOYEE')")
    @Operation(summary = "Get leave application by ID", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<LeaveApplicationResponse>> getLeaveApplicationById(@PathVariable UUID id) {
        LeaveApplicationResponse response = leaveLedgerService.getLeaveApplicationById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

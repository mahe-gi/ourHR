package com.ourhr.payroll;

import com.ourhr.common.ApiResponse;
import com.ourhr.payroll.dto.CreateLopReversalRequest;
import com.ourhr.payroll.dto.LopReversalResponse;
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
@RequestMapping("/api/v1/payroll/lop-reversals")
@RequiredArgsConstructor
public class LopReversalController {

    private final LopReversalService lopReversalService;

    @PostMapping
    @PreAuthorize("hasAnyRole('FOUNDER', 'HR', 'FINANCE')")
    public ResponseEntity<ApiResponse<LopReversalResponse>> createLopReversal(
            @Valid @RequestBody CreateLopReversalRequest request
    ) {
        LopReversalResponse response = lopReversalService.createLopReversal(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('FOUNDER', 'FINANCE')")
    public ResponseEntity<ApiResponse<LopReversalResponse>> approveLopReversal(
            @PathVariable("id") UUID id
    ) {
        LopReversalResponse response = lopReversalService.approveLopReversal(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('FOUNDER', 'HR', 'FINANCE')")
    public ResponseEntity<ApiResponse<List<LopReversalResponse>>> getPendingReversals() {
        List<LopReversalResponse> response = lopReversalService.getPendingReversals();
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

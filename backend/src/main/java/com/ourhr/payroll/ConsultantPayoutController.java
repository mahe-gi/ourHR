package com.ourhr.payroll;

import com.ourhr.common.ApiResponse;
import com.ourhr.payroll.dto.ConsultantPayoutRequest;
import com.ourhr.payroll.dto.ConsultantPayoutResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payroll/consultants")
@RequiredArgsConstructor
public class ConsultantPayoutController {

    private final ConsultantPayoutService consultantPayoutService;

    @PostMapping("/payout")
    @PreAuthorize("hasAnyRole('FOUNDER', 'FINANCE')")
    public ResponseEntity<ApiResponse<ConsultantPayoutResponse>> processConsultantPayout(
            @Valid @RequestBody ConsultantPayoutRequest request
    ) {
        ConsultantPayoutResponse response = consultantPayoutService.processConsultantPayout(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

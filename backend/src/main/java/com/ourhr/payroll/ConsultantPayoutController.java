package com.ourhr.payroll;

import com.ourhr.common.ApiResponse;
import com.ourhr.payroll.dto.ConsultantPayoutRequest;
import com.ourhr.payroll.dto.ConsultantPayoutResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/payroll/consultants")
@RequiredArgsConstructor
public class ConsultantPayoutController {

    private final ConsultantPayoutService consultantPayoutService;

    @GetMapping
    @PreAuthorize("hasAnyRole('FOUNDER', 'HR', 'FINANCE', 'CA')")
    public ResponseEntity<ApiResponse<List<ConsultantPayoutResponse>>> getConsultants() {
        List<ConsultantPayoutResponse> list = consultantPayoutService.getConsultants();
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @PostMapping("/payout")
    @PreAuthorize("hasAnyRole('FOUNDER', 'FINANCE')")
    public ResponseEntity<ApiResponse<ConsultantPayoutResponse>> processConsultantPayout(
            @Valid @RequestBody ConsultantPayoutRequest request
    ) {
        ConsultantPayoutResponse response = consultantPayoutService.processConsultantPayout(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

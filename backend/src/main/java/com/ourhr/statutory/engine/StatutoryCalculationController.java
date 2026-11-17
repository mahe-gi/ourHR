package com.ourhr.statutory.engine;

import com.ourhr.common.ApiResponse;
import com.ourhr.statutory.engine.dto.StatutoryCalculationRequest;
import com.ourhr.statutory.engine.dto.StatutoryCalculationResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/statutory")
@RequiredArgsConstructor
public class StatutoryCalculationController {

    private final IndianStatutoryEngineService statutoryEngineService;

    @PostMapping("/calculate")
    @PreAuthorize("hasAnyRole('FOUNDER', 'HR', 'FINANCE', 'CA')")
    public ResponseEntity<ApiResponse<StatutoryCalculationResult>> calculate(
            @Valid @RequestBody StatutoryCalculationRequest request
    ) {
        StatutoryCalculationResult result = statutoryEngineService.calculate(request);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}

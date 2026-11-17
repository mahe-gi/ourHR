package com.ourhr.employee.salary;

import com.ourhr.common.ApiResponse;
import com.ourhr.employee.salary.dto.PreviousEmployerIncomeResponse;
import com.ourhr.employee.salary.dto.SubmitForm12BRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
@Tag(name = "Form 12B Management", description = "Mid-year joiner previous employer income and tax details")
public class Form12BController {

    private final PreviousEmployerIncomeService previousEmployerIncomeService;

    @PostMapping("/{employeeId}/form-12b")
    @PreAuthorize("hasAnyRole('FOUNDER', 'HR', 'EMPLOYEE')")
    @Operation(summary = "Submit mid-year joiner previous employer income", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<PreviousEmployerIncomeResponse>> savePreviousIncome(
            @PathVariable UUID employeeId,
            @Valid @RequestBody SubmitForm12BRequest request
    ) {
        request.setEmployeeId(employeeId);
        PreviousEmployerIncomeResponse response = previousEmployerIncomeService.savePreviousIncome(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{employeeId}/form-12b/{financialYear}")
    @PreAuthorize("hasAnyRole('FOUNDER', 'HR', 'FINANCE', 'CA', 'EMPLOYEE')")
    @Operation(summary = "Get previous employer income by employee ID and financial year", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<PreviousEmployerIncomeResponse>> getPreviousIncome(
            @PathVariable UUID employeeId,
            @PathVariable String financialYear
    ) {
        PreviousEmployerIncomeResponse response = previousEmployerIncomeService.getPreviousIncome(employeeId, financialYear);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

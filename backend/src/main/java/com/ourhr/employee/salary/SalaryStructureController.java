package com.ourhr.employee.salary;

import com.ourhr.common.ApiResponse;
import com.ourhr.employee.salary.dto.SalaryOfferRequest;
import com.ourhr.employee.salary.dto.SalaryOfferValidationResult;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
@Tag(name = "Salary Structure Management", description = "Salary structure and Code on Wages 2019 validation endpoints")
public class SalaryStructureController {

    private final SalaryStructureService salaryStructureService;
    private final BasicWageValidator basicWageValidator;

    @PostMapping("/{employeeId}/salary-structures")
    @PreAuthorize("hasAnyRole('FOUNDER', 'HR')")
    @Operation(summary = "Create salary structure for employee", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<EmployeeSalaryStructure>> createSalaryStructure(
            @PathVariable UUID employeeId,
            @Valid @RequestBody SalaryOfferRequest request
    ) {
        request.setEmployeeId(employeeId);
        EmployeeSalaryStructure created = salaryStructureService.createSalaryStructure(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(created));
    }

    @PostMapping("/salary-structures/validate")
    @PreAuthorize("hasAnyRole('FOUNDER', 'HR')")
    @Operation(summary = "Validate salary offer against Code on Wages 2019 50% Basic Rule", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<SalaryOfferValidationResult>> validateSalaryStructure(
            @Valid @RequestBody SalaryOfferRequest request
    ) {
        SalaryOfferValidationResult result = basicWageValidator.validate(request);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{employeeId}/salary-structures/active")
    @PreAuthorize("hasAnyRole('FOUNDER', 'HR', 'FINANCE', 'EMPLOYEE')")
    @Operation(summary = "Get active salary structure for employee", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<EmployeeSalaryStructure>> getActiveSalaryStructure(
            @PathVariable UUID employeeId
    ) {
        EmployeeSalaryStructure active = salaryStructureService.getActiveSalaryStructure(employeeId);
        return ResponseEntity.ok(ApiResponse.success(active));
    }
}

package com.ourhr.employee;

import com.ourhr.common.ApiResponse;
import com.ourhr.employee.dto.CreateEmployeeRequest;
import com.ourhr.employee.dto.EmployeeResponse;
import com.ourhr.employee.dto.UpdateEmployeeRequest;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
@Tag(name = "Employee Management", description = "Employee Master endpoints")
public class EmployeeController {

    private final EmployeeService employeeService;

    @PostMapping
    @PreAuthorize("hasAnyRole('FOUNDER', 'HR')")
    @Operation(summary = "Create an employee record", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<EmployeeResponse>> createEmployee(
            @Valid @RequestBody CreateEmployeeRequest request
    ) {
        EmployeeResponse response = employeeService.createEmployee(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('FOUNDER', 'HR', 'FINANCE', 'CA')")
    @Operation(summary = "List all employees for the current tenant", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<List<EmployeeResponse>>> getAllEmployees() {
        List<EmployeeResponse> responses = employeeService.getAllEmployees();
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('FOUNDER', 'HR', 'FINANCE', 'EMPLOYEE')")
    @Operation(summary = "Get employee by ID", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<EmployeeResponse>> getEmployeeById(@PathVariable UUID id) {
        EmployeeResponse response = employeeService.getEmployeeById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('FOUNDER', 'HR')")
    @Operation(summary = "Update employee details", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<EmployeeResponse>> updateEmployee(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateEmployeeRequest request
    ) {
        EmployeeResponse response = employeeService.updateEmployee(id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

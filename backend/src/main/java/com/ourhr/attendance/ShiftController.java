package com.ourhr.attendance;

import com.ourhr.attendance.dto.CreateShiftRequest;
import com.ourhr.attendance.dto.ShiftResponse;
import com.ourhr.common.ApiResponse;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/shifts", "/api/v1/attendance/shifts"})
@RequiredArgsConstructor
@Tag(name = "Shift Management", description = "Endpoints for configuring and viewing tenant work shifts")
public class ShiftController {

    private final ShiftService shiftService;

    @PostMapping
    @PreAuthorize("hasAnyRole('FOUNDER', 'HR')")
    @Operation(summary = "Create a work shift configuration", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<ShiftResponse>> createShift(
            @Valid @RequestBody CreateShiftRequest request
    ) {
        ShiftResponse response = shiftService.createShift(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('FOUNDER', 'HR', 'EMPLOYEE')")
    @Operation(summary = "List all shifts for current tenant", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<List<ShiftResponse>>> getAllShifts() {
        List<ShiftResponse> responses = shiftService.getAllShifts();
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('FOUNDER', 'HR', 'EMPLOYEE')")
    @Operation(summary = "Get shift details by ID", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<ShiftResponse>> getShiftById(@PathVariable UUID id) {
        ShiftResponse response = shiftService.getShiftById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

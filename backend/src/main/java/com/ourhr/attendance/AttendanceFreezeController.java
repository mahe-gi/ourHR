package com.ourhr.attendance;

import com.ourhr.attendance.dto.FreezeAttendanceRequest;
import com.ourhr.attendance.dto.MonthlyAttendanceSummaryResponse;
import com.ourhr.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/attendance/freeze")
@RequiredArgsConstructor
@Tag(name = "Attendance Freeze", description = "Endpoints for freezing monthly attendance summaries and locking payable days for statutory payroll")
public class AttendanceFreezeController {

    private final AttendanceFreezeService attendanceFreezeService;

    @PostMapping
    @PreAuthorize("hasAnyRole('FOUNDER', 'HR', 'FINANCE')")
    @Operation(summary = "Freeze and lock monthly attendance summary for payroll", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<List<MonthlyAttendanceSummaryResponse>>> freezeMonthlyAttendance(
            @Valid @RequestBody FreezeAttendanceRequest request
    ) {
        List<MonthlyAttendanceSummaryResponse> responses = attendanceFreezeService.freezeMonthlyAttendance(request);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('FOUNDER', 'HR', 'FINANCE', 'CA')")
    @Operation(summary = "Retrieve locked monthly attendance summary for payroll run", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<List<MonthlyAttendanceSummaryResponse>>> getMonthlySummary(
            @RequestParam(value = "payrollMonth", required = false) Integer payrollMonth,
            @RequestParam(value = "month", required = false) Integer month,
            @RequestParam(value = "payrollYear", required = false) Integer payrollYear,
            @RequestParam(value = "year", required = false) Integer year
    ) {
        int m = month != null ? month : (payrollMonth != null ? payrollMonth : 0);
        int y = year != null ? year : (payrollYear != null ? payrollYear : 0);

        if (m < 1 || m > 12) {
            throw new IllegalArgumentException("Payroll month must be between 1 and 12");
        }
        if (y < 2020) {
            throw new IllegalArgumentException("Payroll year must be 2020 or later");
        }

        List<MonthlyAttendanceSummaryResponse> responses = attendanceFreezeService.getMonthlySummary(m, y);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }
}

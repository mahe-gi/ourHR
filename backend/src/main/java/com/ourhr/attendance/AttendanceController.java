package com.ourhr.attendance;

import com.ourhr.attendance.dto.PunchRequest;
import com.ourhr.attendance.dto.PunchResponse;
import com.ourhr.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/attendance")
@RequiredArgsConstructor
@Tag(name = "Attendance Management", description = "Endpoints for clock-in/clock-out punch recording and biometric CSV ingestion")
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping("/punch")
    @PreAuthorize("hasAnyRole('FOUNDER', 'HR', 'EMPLOYEE')")
    @Operation(summary = "Record an attendance punch", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<PunchResponse>> recordPunch(
            @Valid @RequestBody PunchRequest request
    ) {
        PunchResponse response = attendanceService.recordPunch(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping(value = "/upload-csv", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, "text/csv", MediaType.APPLICATION_OCTET_STREAM_VALUE, MediaType.ALL_VALUE})
    @PreAuthorize("hasAnyRole('FOUNDER', 'HR')")
    @Operation(summary = "Bulk upload biometric CSV attendance logs", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadBiometricCsv(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestBody(required = false) byte[] rawBytes
    ) throws IOException {
        byte[] content;
        String filename = "biometric.csv";
        if (file != null && !file.isEmpty()) {
            content = file.getBytes();
            filename = file.getOriginalFilename();
        } else if (rawBytes != null && rawBytes.length > 0) {
            content = rawBytes;
        } else {
            throw new IllegalArgumentException("Biometric CSV content cannot be empty");
        }

        int count = attendanceService.uploadBiometricCsv(content, filename);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "processedRecords", count,
                "count", count
        )));
    }

    @GetMapping({"/logs", "/daily"})
    @PreAuthorize("hasAnyRole('FOUNDER', 'HR', 'FINANCE', 'CA', 'EMPLOYEE')")
    @Operation(summary = "Retrieve attendance punch logs", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<List<PunchResponse>>> getAttendanceLogs(
            @RequestParam(value = "employeeId", required = false) UUID employeeId,
            @RequestParam(value = "fromDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(value = "toDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        List<PunchResponse> logs = attendanceService.getAttendanceLogs(employeeId, fromDate, toDate);
        return ResponseEntity.ok(ApiResponse.success(logs));
    }
}

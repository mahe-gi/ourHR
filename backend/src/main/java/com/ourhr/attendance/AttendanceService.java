package com.ourhr.attendance;

import com.ourhr.attendance.dto.PunchRequest;
import com.ourhr.attendance.dto.PunchResponse;
import com.ourhr.auth.User;
import com.ourhr.auth.UserRepository;
import com.ourhr.common.ResourceNotFoundException;
import com.ourhr.common.TenantContext;
import com.ourhr.employee.Employee;
import com.ourhr.employee.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceLogRepository attendanceLogRepository;
    private final MonthlyAttendanceSummaryRepository monthlyAttendanceSummaryRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;

    @Transactional
    public PunchResponse recordPunch(PunchRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant context is required");
        }

        UUID employeeId = resolveEmployeeId(request.getEmployeeId(), tenantId);

        Instant punchTimestamp = request.getPunchTimestamp() != null ? request.getPunchTimestamp() : Instant.now();

        // Check if attendance for this employee in this month/year is already frozen/locked
        ZonedDateTime punchZdt = punchTimestamp.atZone(ZoneId.of("Asia/Kolkata"));
        int punchMonth = punchZdt.getMonthValue();
        int punchYear = punchZdt.getYear();

        monthlyAttendanceSummaryRepository.findByCompanyIdAndEmployeeIdAndPayrollMonthAndPayrollYear(
                tenantId, employeeId, punchMonth, punchYear
        ).ifPresent(summary -> {
            if (Boolean.TRUE.equals(summary.getIsLocked())) {
                throw new IllegalStateException("Attendance for " + punchMonth + "/" + punchYear + " is locked and cannot be modified");
            }
        });

        String punchType = request.getPunchType().trim().toUpperCase();
        if (!"IN".equals(punchType) && !"OUT".equals(punchType)) {
            throw new IllegalArgumentException("Punch type must be IN or OUT");
        }

        String mode = request.getMode() != null ? request.getMode().trim().toUpperCase() : "WEB";

        AttendanceLog log = AttendanceLog.builder()
                .employeeId(employeeId)
                .punchTimestamp(punchTimestamp)
                .punchType(punchType)
                .mode(mode)
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .selfieImageUrl(request.getSelfieImageUrl())
                .isRegularized(false)
                .build();
        log.setCompanyId(tenantId);

        AttendanceLog saved = attendanceLogRepository.save(log);
        return toPunchResponse(saved);
    }

    @Transactional
    public int uploadBiometricCsv(byte[] csvContent, String filename) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant context is required");
        }

        if (csvContent == null || csvContent.length == 0) {
            throw new IllegalArgumentException("CSV content is empty");
        }

        List<AttendanceLog> logsToSave = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new StringReader(new String(csvContent, StandardCharsets.UTF_8)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                String[] parts = line.split(",");
                if (parts.length < 3) {
                    continue;
                }

                String employeeCode = parts[0].replaceAll("^\"|\"$", "").trim();
                String timestampStr = parts[1].replaceAll("^\"|\"$", "").trim();
                String punchType = parts[2].replaceAll("^\"|\"$", "").trim().toUpperCase();

                // Skip header row if detected
                if (isHeaderToken(employeeCode)) {
                    continue;
                }

                if (!"IN".equals(punchType) && !"OUT".equals(punchType)) {
                    throw new IllegalArgumentException("Invalid punch type in CSV: " + punchType + ". Must be IN or OUT");
                }

                Employee employee = employeeRepository.findByCompanyIdAndEmployeeCode(tenantId, employeeCode)
                        .orElseThrow(() -> new IllegalArgumentException("Employee not found with code: " + employeeCode));

                Instant punchTimestamp = parsePunchTimestamp(timestampStr);

                // Verify attendance is not locked for this employee's punch month
                ZonedDateTime punchZdt = punchTimestamp.atZone(ZoneId.of("Asia/Kolkata"));
                int punchMonth = punchZdt.getMonthValue();
                int punchYear = punchZdt.getYear();

                monthlyAttendanceSummaryRepository.findByCompanyIdAndEmployeeIdAndPayrollMonthAndPayrollYear(
                        tenantId, employee.getId(), punchMonth, punchYear
                ).ifPresent(summary -> {
                    if (Boolean.TRUE.equals(summary.getIsLocked())) {
                        throw new IllegalStateException("Attendance for employee " + employeeCode + " in " + punchMonth + "/" + punchYear + " is locked");
                    }
                });

                AttendanceLog log = AttendanceLog.builder()
                        .employeeId(employee.getId())
                        .punchTimestamp(punchTimestamp)
                        .punchType(punchType)
                        .mode("BIOMETRIC_CSV")
                        .isRegularized(false)
                        .build();
                log.setCompanyId(tenantId);

                logsToSave.add(log);
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read CSV content: " + e.getMessage(), e);
        }

        if (!logsToSave.isEmpty()) {
            attendanceLogRepository.saveAll(logsToSave);
        }

        return logsToSave.size();
    }

    @Transactional(readOnly = true)
    public List<PunchResponse> getAttendanceLogs(UUID employeeId, LocalDate fromDate, LocalDate toDate) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant context is required");
        }

        UUID targetEmployeeId = employeeId;
        if (targetEmployeeId == null) {
            targetEmployeeId = tryResolveCurrentEmployeeId(tenantId);
        }

        List<AttendanceLog> logs;

        if (fromDate != null && toDate != null) {
            Instant start = fromDate.atStartOfDay(ZoneId.of("Asia/Kolkata")).toInstant();
            Instant end = toDate.plusDays(1).atStartOfDay(ZoneId.of("Asia/Kolkata")).toInstant();

            if (targetEmployeeId != null) {
                logs = attendanceLogRepository.findByCompanyIdAndEmployeeIdAndPunchTimestampBetween(
                        tenantId, targetEmployeeId, start, end
                );
            } else {
                logs = attendanceLogRepository.findByCompanyIdAndPunchTimestampBetween(
                        tenantId, start, end
                );
            }
        } else if (targetEmployeeId != null) {
            logs = attendanceLogRepository.findByCompanyIdAndEmployeeIdOrderByPunchTimestampDesc(
                    tenantId, targetEmployeeId
            );
        } else {
            logs = List.of();
        }

        return logs.stream().map(this::toPunchResponse).collect(Collectors.toList());
    }

    public PunchResponse toPunchResponse(AttendanceLog log) {
        return PunchResponse.builder()
                .id(log.getId())
                .companyId(log.getCompanyId())
                .employeeId(log.getEmployeeId())
                .punchTimestamp(log.getPunchTimestamp())
                .punchType(log.getPunchType())
                .mode(log.getMode())
                .latitude(log.getLatitude())
                .longitude(log.getLongitude())
                .selfieImageUrl(log.getSelfieImageUrl())
                .isRegularized(Boolean.TRUE.equals(log.getIsRegularized()))
                .createdAt(log.getCreatedAt())
                .build();
    }

    private UUID resolveEmployeeId(UUID requestEmployeeId, UUID tenantId) {
        if (requestEmployeeId != null) {
            var emp = employeeRepository.findByIdAndCompanyId(requestEmployeeId, tenantId);
            if (emp.isPresent()) {
                return requestEmployeeId;
            }
            var empByUser = employeeRepository.findByCompanyIdAndUserId(tenantId, requestEmployeeId);
            if (empByUser.isPresent()) {
                return empByUser.get().getId();
            }
            throw new ResourceNotFoundException("Employee not found with id: " + requestEmployeeId);
        }

        UUID currentEmpId = tryResolveCurrentEmployeeId(tenantId);
        if (currentEmpId != null) {
            return currentEmpId;
        }

        throw new IllegalArgumentException("Employee ID must be provided when current user has no linked employee profile");
    }

    private UUID tryResolveCurrentEmployeeId(UUID tenantId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getName() != null) {
            String email = auth.getName();
            Optional<UUID> direct = userRepository.findByEmail(email)
                    .flatMap(user -> employeeRepository.findByCompanyIdAndUserId(tenantId, user.getId()))
                    .map(Employee::getId);
            if (direct.isPresent()) {
                return direct.get();
            }
        }

        // Fallback for demo/admin/founder: pick first employee in tenant company
        return employeeRepository.findByCompanyId(tenantId).stream()
                .findFirst()
                .map(Employee::getId)
                .orElse(null);
    }

    private boolean isHeaderToken(String token) {
        String lower = token.toLowerCase();
        return lower.equals("employee_code") || lower.equals("employeecode") || lower.equals("employee code") || lower.equals("code");
    }

    private Instant parsePunchTimestamp(String str) {
        if (str == null || str.isBlank()) {
            throw new IllegalArgumentException("Punch timestamp is required");
        }
        String s = str.trim();

        // 1. Try ISO-8601 Instant (e.g. 2026-11-15T09:00:00Z)
        try {
            return Instant.parse(s);
        } catch (DateTimeParseException ignored) {
        }

        // 2. Try OffsetDateTime (e.g. 2026-11-15T09:00:00+05:30)
        try {
            return OffsetDateTime.parse(s).toInstant();
        } catch (DateTimeParseException ignored) {
        }

        // 3. Try yyyy-MM-dd HH:mm:ss
        try {
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            return LocalDateTime.parse(s, dtf).atZone(ZoneId.of("Asia/Kolkata")).toInstant();
        } catch (DateTimeParseException ignored) {
        }

        // 4. Try ISO_LOCAL_DATE_TIME (e.g. 2026-11-15T09:00:00)
        try {
            return LocalDateTime.parse(s, DateTimeFormatter.ISO_LOCAL_DATE_TIME).atZone(ZoneId.of("Asia/Kolkata")).toInstant();
        } catch (DateTimeParseException ignored) {
        }

        throw new IllegalArgumentException("Unable to parse punch timestamp: " + s);
    }
}

package com.ourhr.attendance;

import com.ourhr.attendance.dto.PunchRequest;
import com.ourhr.attendance.dto.PunchResponse;
import com.ourhr.auth.User;
import com.ourhr.auth.UserRepository;
import com.ourhr.common.ResourceNotFoundException;
import com.ourhr.common.TenantContext;
import com.ourhr.employee.Employee;
import com.ourhr.employee.EmployeeRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    @Mock
    private AttendanceLogRepository attendanceLogRepository;

    @Mock
    private MonthlyAttendanceSummaryRepository monthlyAttendanceSummaryRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AttendanceService attendanceService;

    private UUID tenantId;
    private UUID employeeId;
    private Employee sampleEmployee;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        employeeId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);

        sampleEmployee = Employee.builder()
                .employeeCode("EMP-001")
                .firstName("Vikram")
                .lastName("Patel")
                .employmentStatus("ACTIVE")
                .build();
        sampleEmployee.setId(employeeId);
        sampleEmployee.setCompanyId(tenantId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    // =========================================================================
    // 1. Web Punch Recording
    // =========================================================================

    @Test
    @DisplayName("recordPunch: successfully records web punch IN when month is unlocked")
    void recordPunch_webPunchIn_success() {
        Instant now = Instant.parse("2026-11-15T09:05:00Z");
        PunchRequest request = PunchRequest.builder()
                .employeeId(employeeId)
                .punchType("IN")
                .mode("WEB")
                .punchTimestamp(now)
                .build();

        when(employeeRepository.findByIdAndCompanyId(employeeId, tenantId)).thenReturn(Optional.of(sampleEmployee));
        when(monthlyAttendanceSummaryRepository.findByCompanyIdAndEmployeeIdAndPayrollMonthAndPayrollYear(
                tenantId, employeeId, 11, 2026
        )).thenReturn(Optional.empty());

        UUID logId = UUID.randomUUID();
        when(attendanceLogRepository.save(any(AttendanceLog.class))).thenAnswer(invocation -> {
            AttendanceLog log = invocation.getArgument(0);
            log.setId(logId);
            return log;
        });

        PunchResponse response = attendanceService.recordPunch(request);

        assertNotNull(response);
        assertEquals(logId, response.getId());
        assertEquals(employeeId, response.getEmployeeId());
        assertEquals("IN", response.getPunchType());
        assertEquals("WEB", response.getMode());
        assertEquals(now, response.getPunchTimestamp());
        assertFalse(response.isRegularized());

        ArgumentCaptor<AttendanceLog> captor = ArgumentCaptor.forClass(AttendanceLog.class);
        verify(attendanceLogRepository).save(captor.capture());
        AttendanceLog saved = captor.getValue();
        assertEquals(tenantId, saved.getCompanyId());
        assertEquals(employeeId, saved.getEmployeeId());
        assertEquals("IN", saved.getPunchType());
        assertEquals("WEB", saved.getMode());
    }

    // =========================================================================
    // 2. Mobile GPS Punch Recording
    // =========================================================================

    @Test
    @DisplayName("recordPunch: successfully records mobile GPS punch with geo-coordinates and selfie URL")
    void recordPunch_mobileGpsPunchOut_withCoordinatesAndSelfie() {
        Instant now = Instant.parse("2026-11-15T18:30:00Z");
        BigDecimal latitude = new BigDecimal("12.971598");
        BigDecimal longitude = new BigDecimal("77.594566");
        String selfieUrl = "https://s3.ap-south-1.amazonaws.com/ourhr-docs/selfies/emp001-20261115.jpg";

        PunchRequest request = PunchRequest.builder()
                .employeeId(employeeId)
                .punchType("OUT")
                .mode("MOBILE_GPS")
                .latitude(latitude)
                .longitude(longitude)
                .selfieImageUrl(selfieUrl)
                .punchTimestamp(now)
                .build();

        when(employeeRepository.findByIdAndCompanyId(employeeId, tenantId)).thenReturn(Optional.of(sampleEmployee));
        when(monthlyAttendanceSummaryRepository.findByCompanyIdAndEmployeeIdAndPayrollMonthAndPayrollYear(
                tenantId, employeeId, 11, 2026
        )).thenReturn(Optional.empty());

        when(attendanceLogRepository.save(any(AttendanceLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PunchResponse response = attendanceService.recordPunch(request);

        assertNotNull(response);
        assertEquals("OUT", response.getPunchType());
        assertEquals("MOBILE_GPS", response.getMode());
        assertEquals(latitude, response.getLatitude());
        assertEquals(longitude, response.getLongitude());
        assertEquals(selfieUrl, response.getSelfieImageUrl());

        ArgumentCaptor<AttendanceLog> captor = ArgumentCaptor.forClass(AttendanceLog.class);
        verify(attendanceLogRepository).save(captor.capture());
        AttendanceLog saved = captor.getValue();
        assertEquals(latitude, saved.getLatitude());
        assertEquals(longitude, saved.getLongitude());
        assertEquals(selfieUrl, saved.getSelfieImageUrl());
    }

    // =========================================================================
    // 3. Locked Month Rejection
    // =========================================================================

    @Test
    @DisplayName("recordPunch: throws IllegalStateException when payroll month attendance is already locked")
    void recordPunch_throwsWhenAttendanceLocked() {
        Instant now = Instant.parse("2026-10-15T09:00:00Z");
        PunchRequest request = PunchRequest.builder()
                .employeeId(employeeId)
                .punchType("IN")
                .mode("WEB")
                .punchTimestamp(now)
                .build();

        MonthlyAttendanceSummary lockedSummary = new MonthlyAttendanceSummary();
        lockedSummary.setIsLocked(true);
        lockedSummary.setLockedAt(Instant.now());

        when(employeeRepository.findByIdAndCompanyId(employeeId, tenantId)).thenReturn(Optional.of(sampleEmployee));
        when(monthlyAttendanceSummaryRepository.findByCompanyIdAndEmployeeIdAndPayrollMonthAndPayrollYear(
                tenantId, employeeId, 10, 2026
        )).thenReturn(Optional.of(lockedSummary));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                attendanceService.recordPunch(request)
        );

        assertTrue(ex.getMessage().contains("locked and cannot be modified"));
        verify(attendanceLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("recordPunch: throws IllegalArgumentException when punch type is not IN or OUT")
    void recordPunch_throwsWhenPunchTypeInvalid() {
        PunchRequest request = PunchRequest.builder()
                .employeeId(employeeId)
                .punchType("BREAK")
                .mode("WEB")
                .build();

        when(employeeRepository.findByIdAndCompanyId(employeeId, tenantId)).thenReturn(Optional.of(sampleEmployee));

        assertThrows(IllegalArgumentException.class, () -> attendanceService.recordPunch(request));
    }

    // =========================================================================
    // 4. Biometric CSV Ingestion
    // =========================================================================

    @Test
    @DisplayName("uploadBiometricCsv: parses CSV lines and saves AttendanceLog records with mode BIOMETRIC_CSV")
    void uploadBiometricCsv_success() {
        String csv = "employee_code,punch_timestamp,punch_type\n" +
                "EMP-001,2026-11-15T09:00:00Z,IN\n" +
                "EMP-001,2026-11-15T18:00:00Z,OUT\n";

        when(employeeRepository.findByCompanyIdAndEmployeeCode(tenantId, "EMP-001"))
                .thenReturn(Optional.of(sampleEmployee));
        when(monthlyAttendanceSummaryRepository.findByCompanyIdAndEmployeeIdAndPayrollMonthAndPayrollYear(
                tenantId, employeeId, 11, 2026
        )).thenReturn(Optional.empty());

        int count = attendanceService.uploadBiometricCsv(csv.getBytes(StandardCharsets.UTF_8), "attendance_nov.csv");

        assertEquals(2, count);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<AttendanceLog>> captor = ArgumentCaptor.forClass(List.class);
        verify(attendanceLogRepository).saveAll(captor.capture());

        List<AttendanceLog> savedList = captor.getValue();
        assertEquals(2, savedList.size());
        assertEquals("BIOMETRIC_CSV", savedList.get(0).getMode());
        assertEquals("IN", savedList.get(0).getPunchType());
        assertEquals(Instant.parse("2026-11-15T09:00:00Z"), savedList.get(0).getPunchTimestamp());
        assertEquals("BIOMETRIC_CSV", savedList.get(1).getMode());
        assertEquals("OUT", savedList.get(1).getPunchType());
        assertEquals(Instant.parse("2026-11-15T18:00:00Z"), savedList.get(1).getPunchTimestamp());
    }

    @Test
    @DisplayName("uploadBiometricCsv: throws IllegalStateException if CSV contains punches for locked month")
    void uploadBiometricCsv_throwsWhenMonthLocked() {
        String csv = "employee_code,punch_timestamp,punch_type\n" +
                "EMP-001,2026-10-15T09:00:00Z,IN\n";

        MonthlyAttendanceSummary lockedSummary = new MonthlyAttendanceSummary();
        lockedSummary.setIsLocked(true);

        when(employeeRepository.findByCompanyIdAndEmployeeCode(tenantId, "EMP-001"))
                .thenReturn(Optional.of(sampleEmployee));
        when(monthlyAttendanceSummaryRepository.findByCompanyIdAndEmployeeIdAndPayrollMonthAndPayrollYear(
                tenantId, employeeId, 10, 2026
        )).thenReturn(Optional.of(lockedSummary));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                attendanceService.uploadBiometricCsv(csv.getBytes(StandardCharsets.UTF_8), "attendance_oct.csv")
        );

        assertTrue(ex.getMessage().contains("locked"));
        verify(attendanceLogRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("uploadBiometricCsv: throws IllegalArgumentException if employee code is unknown")
    void uploadBiometricCsv_throwsWhenEmployeeNotFound() {
        String csv = "employee_code,punch_timestamp,punch_type\n" +
                "UNKNOWN-999,2026-11-15T09:00:00Z,IN\n";

        when(employeeRepository.findByCompanyIdAndEmployeeCode(tenantId, "UNKNOWN-999"))
                .thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                attendanceService.uploadBiometricCsv(csv.getBytes(StandardCharsets.UTF_8), "attendance.csv")
        );
    }

    @Test
    @DisplayName("uploadBiometricCsv: throws IllegalArgumentException for empty content")
    void uploadBiometricCsv_throwsForEmptyContent() {
        assertThrows(IllegalArgumentException.class, () ->
                attendanceService.uploadBiometricCsv(new byte[0], "empty.csv")
        );
    }

    // =========================================================================
    // 5. Query Logs & Tenant Context Validation
    // =========================================================================

    @Test
    @DisplayName("getAttendanceLogs: filters by employee and date range")
    void getAttendanceLogs_filtersByDateRange() {
        LocalDate fromDate = LocalDate.of(2026, 11, 1);
        LocalDate toDate = LocalDate.of(2026, 11, 30);
        Instant start = fromDate.atStartOfDay(ZoneId.of("Asia/Kolkata")).toInstant();
        Instant end = toDate.plusDays(1).atStartOfDay(ZoneId.of("Asia/Kolkata")).toInstant();

        AttendanceLog log = AttendanceLog.builder()
                .employeeId(employeeId)
                .punchTimestamp(Instant.parse("2026-11-15T09:00:00Z"))
                .punchType("IN")
                .mode("WEB")
                .build();
        log.setId(UUID.randomUUID());
        log.setCompanyId(tenantId);

        when(attendanceLogRepository.findByCompanyIdAndEmployeeIdAndPunchTimestampBetween(tenantId, employeeId, start, end))
                .thenReturn(List.of(log));

        List<PunchResponse> logs = attendanceService.getAttendanceLogs(employeeId, fromDate, toDate);

        assertEquals(1, logs.size());
        assertEquals("IN", logs.get(0).getPunchType());
    }

    @Test
    @DisplayName("Throws IllegalArgumentException when tenant context is missing")
    void throwsWhenTenantContextMissing() {
        TenantContext.clear();

        PunchRequest request = PunchRequest.builder().employeeId(employeeId).punchType("IN").build();
        assertThrows(IllegalArgumentException.class, () -> attendanceService.recordPunch(request));
        assertThrows(IllegalArgumentException.class, () -> attendanceService.uploadBiometricCsv("data".getBytes(), "f.csv"));
        assertThrows(IllegalArgumentException.class, () -> attendanceService.getAttendanceLogs(employeeId, null, null));
    }
}

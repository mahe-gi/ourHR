package com.ourhr.attendance;

import com.ourhr.attendance.dto.FreezeAttendanceRequest;
import com.ourhr.attendance.dto.MonthlyAttendanceSummaryResponse;
import com.ourhr.common.TenantContext;
import com.ourhr.company.Branch;
import com.ourhr.company.BranchRepository;
import com.ourhr.employee.Employee;
import com.ourhr.employee.EmployeeRepository;
import com.ourhr.leave.LeaveApplication;
import com.ourhr.leave.LeaveApplicationRepository;
import com.ourhr.leave.LeaveType;
import com.ourhr.leave.LeaveTypeRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttendanceFreezeServiceTest {

    @Mock
    private MonthlyAttendanceSummaryRepository monthlyAttendanceSummaryRepository;

    @Mock
    private AttendanceLogRepository attendanceLogRepository;

    @Mock
    private LeaveApplicationRepository leaveApplicationRepository;

    @Mock
    private LeaveTypeRepository leaveTypeRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private BranchRepository branchRepository;

    @Mock
    private HolidayCalendarRepository holidayCalendarRepository;

    @InjectMocks
    private AttendanceFreezeService attendanceFreezeService;

    private UUID tenantId;
    private UUID employeeId;
    private UUID branchId;
    private Employee sampleEmployee;
    private Branch sampleBranch;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        employeeId = UUID.randomUUID();
        branchId = UUID.randomUUID();

        TenantContext.setTenantId(tenantId);

        sampleBranch = Branch.builder()
                .branchName("Pune Development Center")
                .city("Pune")
                .addressLine("Tech Park, Hinjawadi")
                .pincode("411057")
                .stateCode("MH")
                .build();
        sampleBranch.setId(branchId);
        sampleBranch.setCompanyId(tenantId);

        sampleEmployee = Employee.builder()
                .employeeCode("EMP-101")
                .firstName("Aditi")
                .lastName("Deshmukh")
                .branchId(branchId)
                .employmentStatus("ACTIVE")
                .build();
        sampleEmployee.setId(employeeId);
        sampleEmployee.setCompanyId(tenantId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // =========================================================================
    // 1. Freeze Attendance and Compute Payable / LOP Days
    // =========================================================================

    @Test
    @DisplayName("freezeMonthlyAttendance: calculates lopDays, payableDays, sets isLocked=true and lockedAt for active employees")
    void freezeMonthlyAttendance_computesPayableDaysAndLocksSummary() {
        FreezeAttendanceRequest request = FreezeAttendanceRequest.builder()
                .payrollMonth(10)
                .payrollYear(2026)
                .build();

        // Month: October 2026 -> 31 total calendar days, 9 weekend days (5 Sat, 4 Sun)
        LocalDate monthStart = LocalDate.of(2026, 10, 1);
        LocalDate monthEnd = LocalDate.of(2026, 10, 31);
        Instant startInstant = monthStart.atStartOfDay(ZoneId.of("Asia/Kolkata")).toInstant();
        Instant endInstant = monthEnd.plusDays(1).atStartOfDay(ZoneId.of("Asia/Kolkata")).toInstant();

        when(employeeRepository.findByCompanyId(tenantId)).thenReturn(List.of(sampleEmployee));
        when(branchRepository.findByIdAndCompanyId(branchId, tenantId)).thenReturn(Optional.of(sampleBranch));

        // 1 state public holiday on Friday Oct 2 (Gandhi Jayanti)
        HolidayCalendar holiday = HolidayCalendar.builder()
                .holidayName("Gandhi Jayanti")
                .holidayDate(LocalDate.of(2026, 10, 2))
                .stateCode("MH")
                .isRestricted(false)
                .build();
        when(holidayCalendarRepository.findHolidaysBetween(tenantId, "MH", monthStart, monthEnd))
                .thenReturn(List.of(holiday));

        // 15 worked days (punches on 15 distinct working days)
        List<AttendanceLog> punchLogs = new ArrayList<>();
        // Dates: Oct 5 to Oct 9 (5 days), Oct 12 to Oct 16 (5 days), Oct 19 to Oct 23 (5 days) = 15 working days
        for (int day : List.of(5, 6, 7, 8, 9, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23)) {
            punchLogs.add(AttendanceLog.builder()
                    .employeeId(employeeId)
                    .punchTimestamp(LocalDate.of(2026, 10, day).atTime(9, 0).atZone(ZoneId.of("Asia/Kolkata")).toInstant())
                    .punchType("IN")
                    .mode("WEB")
                    .build());
        }
        when(attendanceLogRepository.findByCompanyIdAndEmployeeIdAndPunchTimestampBetween(
                tenantId, employeeId, startInstant, endInstant
        )).thenReturn(punchLogs);

        // 2 days approved leave (Oct 26 and Oct 27)
        UUID leaveTypeId = UUID.randomUUID();
        LeaveApplication approvedLeave = LeaveApplication.builder()
                .employeeId(employeeId)
                .leaveTypeId(leaveTypeId)
                .startDate(LocalDate.of(2026, 10, 26))
                .endDate(LocalDate.of(2026, 10, 27))
                .totalDays(new BigDecimal("2.0"))
                .status("APPROVED")
                .build();
        approvedLeave.setId(UUID.randomUUID());
        approvedLeave.setCompanyId(tenantId);

        when(leaveApplicationRepository.findByCompanyIdAndEmployeeIdAndStatus(tenantId, employeeId, "APPROVED"))
                .thenReturn(List.of(approvedLeave));

        when(monthlyAttendanceSummaryRepository.findByCompanyIdAndEmployeeIdAndPayrollMonthAndPayrollYear(
                tenantId, employeeId, 10, 2026
        )).thenReturn(Optional.empty());

        when(monthlyAttendanceSummaryRepository.save(any(MonthlyAttendanceSummary.class)))
                .thenAnswer(invocation -> {
                    MonthlyAttendanceSummary s = invocation.getArgument(0);
                    s.setId(UUID.randomUUID());
                    return s;
                });

        List<MonthlyAttendanceSummaryResponse> responses = attendanceFreezeService.freezeMonthlyAttendance(request);

        assertNotNull(responses);
        assertEquals(1, responses.size());
        MonthlyAttendanceSummaryResponse summary = responses.get(0);
        assertEquals(employeeId, summary.getEmployeeId());
        assertEquals("EMP-101", summary.getEmployeeCode());
        assertEquals(10, summary.getPayrollMonth());
        assertEquals(2026, summary.getPayrollYear());
        assertEquals(31, summary.getTotalCalendarDays());

        // Arithmetic breakdown:
        // Total calendar days = 31
        // Worked days = 15
        // Weekends = 9 (Oct 3, 4, 10, 11, 17, 18, 24, 25, 31)
        // Public holidays = 1 (Oct 2)
        // Approved leaves = 2 (Oct 26, 27)
        // Sum credits = 15 + 9 + 1 + 2 = 27
        // LOP days = 31 - 27 = 4.0
        // Payable days = 31 - 4.0 = 27.0
        assertEquals(new BigDecimal("27.0"), summary.getPayableDays());
        assertEquals(new BigDecimal("4.0"), summary.getLopDays());
        assertTrue(summary.isLocked());
        assertNotNull(summary.getLockedAt());

        ArgumentCaptor<MonthlyAttendanceSummary> captor = ArgumentCaptor.forClass(MonthlyAttendanceSummary.class);
        verify(monthlyAttendanceSummaryRepository).save(captor.capture());
        MonthlyAttendanceSummary savedEntity = captor.getValue();
        assertEquals(31, savedEntity.getTotalCalendarDays());
        assertEquals(new BigDecimal("27.0"), savedEntity.getPayableDays());
        assertEquals(new BigDecimal("4.0"), savedEntity.getLopDays());
        assertTrue(savedEntity.getIsLocked());
        assertNotNull(savedEntity.getLockedAt());
    }

    @Test
    @DisplayName("freezeMonthlyAttendance: zero lopDays when worked, leaves, weekends, and holidays cover full month")
    void freezeMonthlyAttendance_zeroLopDays_fullMonthCovered() {
        FreezeAttendanceRequest request = FreezeAttendanceRequest.builder()
                .payrollMonth(10)
                .payrollYear(2026)
                .build();

        LocalDate monthStart = LocalDate.of(2026, 10, 1);
        LocalDate monthEnd = LocalDate.of(2026, 10, 31);
        Instant startInstant = monthStart.atStartOfDay(ZoneId.of("Asia/Kolkata")).toInstant();
        Instant endInstant = monthEnd.plusDays(1).atStartOfDay(ZoneId.of("Asia/Kolkata")).toInstant();

        when(employeeRepository.findByCompanyId(tenantId)).thenReturn(List.of(sampleEmployee));
        when(branchRepository.findByIdAndCompanyId(branchId, tenantId)).thenReturn(Optional.of(sampleBranch));
        when(holidayCalendarRepository.findHolidaysBetween(tenantId, "MH", monthStart, monthEnd)).thenReturn(List.of());

        // 22 working days in October 2026 (31 total - 9 weekends = 22 weekdays)
        // Simulate punches for all 22 weekdays
        List<AttendanceLog> punchLogs = new ArrayList<>();
        for (LocalDate d = monthStart; !d.isAfter(monthEnd); d = d.plusDays(1)) {
            if (d.getDayOfWeek().getValue() < 6) { // Monday to Friday
                punchLogs.add(AttendanceLog.builder()
                        .employeeId(employeeId)
                        .punchTimestamp(d.atTime(9, 0).atZone(ZoneId.of("Asia/Kolkata")).toInstant())
                        .punchType("IN")
                        .mode("WEB")
                        .build());
            }
        }
        when(attendanceLogRepository.findByCompanyIdAndEmployeeIdAndPunchTimestampBetween(
                tenantId, employeeId, startInstant, endInstant
        )).thenReturn(punchLogs);

        when(leaveApplicationRepository.findByCompanyIdAndEmployeeIdAndStatus(tenantId, employeeId, "APPROVED"))
                .thenReturn(List.of());

        when(monthlyAttendanceSummaryRepository.findByCompanyIdAndEmployeeIdAndPayrollMonthAndPayrollYear(
                tenantId, employeeId, 10, 2026
        )).thenReturn(Optional.empty());

        when(monthlyAttendanceSummaryRepository.save(any(MonthlyAttendanceSummary.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<MonthlyAttendanceSummaryResponse> responses = attendanceFreezeService.freezeMonthlyAttendance(request);

        assertNotNull(responses);
        assertEquals(1, responses.size());
        MonthlyAttendanceSummaryResponse summary = responses.get(0);
        // Worked = 22, Weekends = 9 -> Sum = 31 -> LOP = 0.0, Payable = 31.0
        assertEquals(new BigDecimal("0.0"), summary.getLopDays());
        assertEquals(new BigDecimal("31.0"), summary.getPayableDays());
        assertTrue(summary.isLocked());
    }

    // =========================================================================
    // 2. Validation & Boundary Checks
    // =========================================================================

    @Test
    @DisplayName("freezeMonthlyAttendance: throws IllegalArgumentException when month or year is invalid")
    void freezeMonthlyAttendance_throwsWhenInputInvalid() {
        assertThrows(IllegalArgumentException.class, () ->
                attendanceFreezeService.freezeMonthlyAttendance(
                        FreezeAttendanceRequest.builder().payrollMonth(0).payrollYear(2026).build()
                )
        );

        assertThrows(IllegalArgumentException.class, () ->
                attendanceFreezeService.freezeMonthlyAttendance(
                        FreezeAttendanceRequest.builder().payrollMonth(13).payrollYear(2026).build()
                )
        );

        assertThrows(IllegalArgumentException.class, () ->
                attendanceFreezeService.freezeMonthlyAttendance(
                        FreezeAttendanceRequest.builder().payrollMonth(10).payrollYear(2019).build()
                )
        );
    }

    @Test
    @DisplayName("getMonthlySummary: returns summaries for current tenant")
    void getMonthlySummary_returnsList() {
        MonthlyAttendanceSummary s = new MonthlyAttendanceSummary();
        s.setId(UUID.randomUUID());
        s.setCompanyId(tenantId);
        s.setEmployeeId(employeeId);
        s.setPayrollMonth(10);
        s.setPayrollYear(2026);
        s.setTotalCalendarDays(31);
        s.setPayableDays(new BigDecimal("31.0"));
        s.setLopDays(BigDecimal.ZERO);
        s.setIsLocked(true);
        s.setLockedAt(Instant.now());

        when(monthlyAttendanceSummaryRepository.findByCompanyIdAndPayrollMonthAndPayrollYear(tenantId, 10, 2026))
                .thenReturn(List.of(s));
        when(employeeRepository.findByCompanyId(tenantId)).thenReturn(List.of(sampleEmployee));

        List<MonthlyAttendanceSummaryResponse> list = attendanceFreezeService.getMonthlySummary(10, 2026);

        assertEquals(1, list.size());
        assertEquals("EMP-101", list.get(0).getEmployeeCode());
        assertTrue(list.get(0).isLocked());
    }

    @Test
    @DisplayName("Throws IllegalArgumentException when tenant context is missing")
    void throwsWhenTenantContextMissing() {
        TenantContext.clear();

        FreezeAttendanceRequest request = FreezeAttendanceRequest.builder().payrollMonth(10).payrollYear(2026).build();
        assertThrows(IllegalArgumentException.class, () -> attendanceFreezeService.freezeMonthlyAttendance(request));
        assertThrows(IllegalArgumentException.class, () -> attendanceFreezeService.getMonthlySummary(10, 2026));
    }
}

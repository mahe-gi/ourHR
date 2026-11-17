package com.ourhr.attendance;

import com.ourhr.attendance.dto.FreezeAttendanceRequest;
import com.ourhr.attendance.dto.MonthlyAttendanceSummaryResponse;
import com.ourhr.company.Branch;
import com.ourhr.company.BranchRepository;
import com.ourhr.common.TenantContext;
import com.ourhr.employee.Employee;
import com.ourhr.employee.EmployeeRepository;
import com.ourhr.leave.LeaveApplication;
import com.ourhr.leave.LeaveApplicationRepository;
import com.ourhr.leave.LeaveType;
import com.ourhr.leave.LeaveTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttendanceFreezeService {

    private final MonthlyAttendanceSummaryRepository monthlyAttendanceSummaryRepository;
    private final AttendanceLogRepository attendanceLogRepository;
    private final LeaveApplicationRepository leaveApplicationRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final EmployeeRepository employeeRepository;
    private final BranchRepository branchRepository;
    private final HolidayCalendarRepository holidayCalendarRepository;

    @Transactional
    public List<MonthlyAttendanceSummaryResponse> freezeMonthlyAttendance(FreezeAttendanceRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant context is required");
        }

        int month = request.getPayrollMonth();
        int year = request.getPayrollYear();

        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("Payroll month must be between 1 and 12");
        }
        if (year < 2020) {
            throw new IllegalArgumentException("Payroll year must be 2020 or later");
        }

        YearMonth yearMonth = YearMonth.of(year, month);
        int totalCalendarDays = yearMonth.lengthOfMonth();
        LocalDate monthStart = yearMonth.atDay(1);
        LocalDate monthEnd = yearMonth.atEndOfMonth();

        Instant startInstant = monthStart.atStartOfDay(ZoneId.of("Asia/Kolkata")).toInstant();
        Instant endInstant = monthEnd.plusDays(1).atStartOfDay(ZoneId.of("Asia/Kolkata")).toInstant();

        // Count weekends in the month
        int weekends = 0;
        Set<LocalDate> weekendDates = new HashSet<>();
        for (LocalDate d = monthStart; !d.isAfter(monthEnd); d = d.plusDays(1)) {
            if (d.getDayOfWeek() == DayOfWeek.SATURDAY || d.getDayOfWeek() == DayOfWeek.SUNDAY) {
                weekends++;
                weekendDates.add(d);
            }
        }

        List<Employee> activeEmployees = employeeRepository.findByCompanyId(tenantId).stream()
                .filter(e -> "ACTIVE".equalsIgnoreCase(e.getEmploymentStatus()))
                .collect(Collectors.toList());

        List<MonthlyAttendanceSummaryResponse> responses = new ArrayList<>();

        for (Employee emp : activeEmployees) {
            // Worked days from punches
            List<AttendanceLog> logs = attendanceLogRepository.findByCompanyIdAndEmployeeIdAndPunchTimestampBetween(
                    tenantId, emp.getId(), startInstant, endInstant
            );
            Set<LocalDate> workedDates = logs.stream()
                    .map(l -> l.getPunchTimestamp().atZone(ZoneId.of("Asia/Kolkata")).toLocalDate())
                    .filter(d -> !d.isBefore(monthStart) && !d.isAfter(monthEnd))
                    .collect(Collectors.toSet());
            int workedDays = workedDates.size();

            // Branch state code for holiday lookup
            String stateCode = "ALL";
            if (emp.getBranchId() != null) {
                stateCode = branchRepository.findByIdAndCompanyId(emp.getBranchId(), tenantId)
                        .map(Branch::getStateCode)
                        .orElse("ALL");
            }

            List<HolidayCalendar> holidays = holidayCalendarRepository.findHolidaysBetween(tenantId, stateCode, monthStart, monthEnd);
            Set<LocalDate> holidayDates = holidays.stream()
                    .map(HolidayCalendar::getHolidayDate)
                    .filter(d -> !d.isBefore(monthStart) && !d.isAfter(monthEnd))
                    .filter(d -> !weekendDates.contains(d))
                    .collect(Collectors.toSet());
            int holidayCount = holidayDates.size();

            // Approved leave days in month
            List<LeaveApplication> approvedLeaves = leaveApplicationRepository
                    .findByCompanyIdAndEmployeeIdAndStatus(tenantId, emp.getId(), "APPROVED");
            BigDecimal approvedLeaveDays = BigDecimal.ZERO;

            for (LeaveApplication app : approvedLeaves) {
                LocalDate appStart = app.getStartDate();
                LocalDate appEnd = app.getEndDate();

                if (appStart.isAfter(monthEnd) || appEnd.isBefore(monthStart)) {
                    continue;
                }

                if (!appStart.isBefore(monthStart) && !appEnd.isAfter(monthEnd)) {
                    approvedLeaveDays = approvedLeaveDays.add(app.getTotalDays());
                } else {
                    LocalDate overlapStart = appStart.isBefore(monthStart) ? monthStart : appStart;
                    LocalDate overlapEnd = appEnd.isAfter(monthEnd) ? monthEnd : appEnd;

                    LeaveType lt = leaveTypeRepository.findByIdAndCompanyId(app.getLeaveTypeId(), tenantId).orElse(null);
                    if (lt != null && Boolean.TRUE.equals(lt.getIsSandwichRuleEnabled())) {
                        long days = ChronoUnit.DAYS.between(overlapStart, overlapEnd) + 1;
                        approvedLeaveDays = approvedLeaveDays.add(BigDecimal.valueOf(days));
                    } else {
                        long workingDays = 0;
                        for (LocalDate d = overlapStart; !d.isAfter(overlapEnd); d = d.plusDays(1)) {
                            if (d.getDayOfWeek() != DayOfWeek.SATURDAY && d.getDayOfWeek() != DayOfWeek.SUNDAY && !holidayDates.contains(d)) {
                                workingDays++;
                            }
                        }
                        approvedLeaveDays = approvedLeaveDays.add(BigDecimal.valueOf(workingDays));
                    }
                }
            }

            // Calculations
            BigDecimal totalDaysBd = BigDecimal.valueOf(totalCalendarDays).setScale(1, RoundingMode.HALF_UP);
            BigDecimal workedDaysBd = BigDecimal.valueOf(workedDays).setScale(1, RoundingMode.HALF_UP);
            BigDecimal weekendsBd = BigDecimal.valueOf(weekends).setScale(1, RoundingMode.HALF_UP);
            BigDecimal holidaysBd = BigDecimal.valueOf(holidayCount).setScale(1, RoundingMode.HALF_UP);

            BigDecimal sumCredits = workedDaysBd.add(approvedLeaveDays).add(weekendsBd).add(holidaysBd);
            BigDecimal lopDays;
            if (sumCredits.compareTo(totalDaysBd) >= 0) {
                lopDays = BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
            } else {
                lopDays = totalDaysBd.subtract(sumCredits).setScale(1, RoundingMode.HALF_UP);
            }
            BigDecimal payableDays = totalDaysBd.subtract(lopDays).setScale(1, RoundingMode.HALF_UP);

            MonthlyAttendanceSummary summary = monthlyAttendanceSummaryRepository
                    .findByCompanyIdAndEmployeeIdAndPayrollMonthAndPayrollYear(tenantId, emp.getId(), month, year)
                    .orElseGet(() -> {
                        MonthlyAttendanceSummary s = new MonthlyAttendanceSummary();
                        s.setCompanyId(tenantId);
                        s.setEmployeeId(emp.getId());
                        s.setPayrollMonth(month);
                        s.setPayrollYear(year);
                        return s;
                    });

            summary.setTotalCalendarDays(totalCalendarDays);
            summary.setPayableDays(payableDays);
            summary.setLopDays(lopDays);
            summary.setOvertimeHours(summary.getOvertimeHours() != null ? summary.getOvertimeHours() : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            summary.setIsLocked(true);
            summary.setLockedAt(Instant.now());

            MonthlyAttendanceSummary saved = monthlyAttendanceSummaryRepository.save(summary);

            responses.add(MonthlyAttendanceSummaryResponse.builder()
                    .id(saved.getId())
                    .companyId(saved.getCompanyId())
                    .employeeId(saved.getEmployeeId())
                    .employeeCode(emp.getEmployeeCode())
                    .employeeName(emp.getFirstName() + " " + emp.getLastName())
                    .payrollMonth(saved.getPayrollMonth())
                    .payrollYear(saved.getPayrollYear())
                    .totalCalendarDays(saved.getTotalCalendarDays())
                    .payableDays(saved.getPayableDays())
                    .lopDays(saved.getLopDays())
                    .overtimeHours(saved.getOvertimeHours())
                    .isLocked(Boolean.TRUE.equals(saved.getIsLocked()))
                    .lockedAt(saved.getLockedAt())
                    .createdAt(saved.getCreatedAt())
                    .build());
        }

        return responses;
    }

    @Transactional(readOnly = true)
    public List<MonthlyAttendanceSummaryResponse> getMonthlySummary(int month, int year) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant context is required");
        }

        List<MonthlyAttendanceSummary> summaries = monthlyAttendanceSummaryRepository
                .findByCompanyIdAndPayrollMonthAndPayrollYear(tenantId, month, year);

        Map<UUID, Employee> employeeMap = employeeRepository.findByCompanyId(tenantId).stream()
                .collect(Collectors.toMap(Employee::getId, e -> e));

        return summaries.stream().map(s -> {
            Employee emp = employeeMap.get(s.getEmployeeId());
            return MonthlyAttendanceSummaryResponse.builder()
                    .id(s.getId())
                    .companyId(s.getCompanyId())
                    .employeeId(s.getEmployeeId())
                    .employeeCode(emp != null ? emp.getEmployeeCode() : null)
                    .employeeName(emp != null ? emp.getFirstName() + " " + emp.getLastName() : null)
                    .payrollMonth(s.getPayrollMonth())
                    .payrollYear(s.getPayrollYear())
                    .totalCalendarDays(s.getTotalCalendarDays())
                    .payableDays(s.getPayableDays())
                    .lopDays(s.getLopDays())
                    .overtimeHours(s.getOvertimeHours())
                    .isLocked(Boolean.TRUE.equals(s.getIsLocked()))
                    .lockedAt(s.getLockedAt())
                    .createdAt(s.getCreatedAt())
                    .build();
        }).collect(Collectors.toList());
    }
}

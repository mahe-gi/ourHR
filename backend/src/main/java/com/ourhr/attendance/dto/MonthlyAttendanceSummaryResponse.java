package com.ourhr.attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyAttendanceSummaryResponse {

    private UUID id;
    private UUID companyId;
    private UUID employeeId;
    private String employeeCode;
    private String employeeName;
    private int payrollMonth;
    private int payrollYear;
    private int totalCalendarDays;
    private BigDecimal payableDays;
    private BigDecimal lopDays;
    private BigDecimal overtimeHours;
    private boolean isLocked;
    private Instant lockedAt;
    private Instant createdAt;
}

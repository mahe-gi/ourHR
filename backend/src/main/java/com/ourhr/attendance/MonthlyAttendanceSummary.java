package com.ourhr.attendance;

import com.ourhr.common.BaseTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "monthly_attendance_summary")
public class MonthlyAttendanceSummary extends BaseTenantEntity {

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(name = "payroll_month", nullable = false)
    private Integer payrollMonth;

    @Column(name = "payroll_year", nullable = false)
    private Integer payrollYear;

    @Column(name = "total_calendar_days", nullable = false)
    private Integer totalCalendarDays;

    @Column(name = "payable_days", precision = 4, scale = 1, nullable = false)
    private BigDecimal payableDays;

    @Builder.Default
    @Column(name = "lop_days", precision = 4, scale = 1, nullable = false)
    private BigDecimal lopDays = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "overtime_hours", precision = 5, scale = 2, nullable = false)
    private BigDecimal overtimeHours = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "is_locked", nullable = false)
    private Boolean isLocked = false;

    @Column(name = "locked_at")
    private Instant lockedAt;
}

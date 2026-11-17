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
import java.time.LocalTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "shifts")
public class Shift extends BaseTenantEntity {

    @Column(name = "shift_name", length = 100, nullable = false)
    private String shiftName;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Builder.Default
    @Column(name = "grace_minutes", nullable = false)
    private Integer graceMinutes = 15;

    @Builder.Default
    @Column(name = "half_day_threshold_hours", precision = 4, scale = 2, nullable = false)
    private BigDecimal halfDayThresholdHours = new BigDecimal("4.50");

    @Builder.Default
    @Column(name = "full_day_threshold_hours", precision = 4, scale = 2, nullable = false)
    private BigDecimal fullDayThresholdHours = new BigDecimal("8.00");

    @Builder.Default
    @Column(name = "late_mark_penalty_count", nullable = false)
    private Integer lateMarkPenaltyCount = 3;

    @Builder.Default
    @Column(name = "late_mark_penalty_deduction_days", precision = 3, scale = 1, nullable = false)
    private BigDecimal lateMarkPenaltyDeductionDays = new BigDecimal("0.5");
}

package com.ourhr.attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShiftResponse {

    private UUID id;
    private UUID companyId;
    private String shiftName;
    private LocalTime startTime;
    private LocalTime endTime;
    private int graceMinutes;
    private BigDecimal halfDayThresholdHours;
    private BigDecimal fullDayThresholdHours;
    private int lateMarkPenaltyCount;
    private BigDecimal lateMarkPenaltyDeductionDays;
    private Instant createdAt;
}

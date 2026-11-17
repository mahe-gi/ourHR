package com.ourhr.attendance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateShiftRequest {

    @NotBlank(message = "Shift name is required")
    @Size(max = 100, message = "Shift name cannot exceed 100 characters")
    private String shiftName;

    @NotNull(message = "Start time is required")
    private LocalTime startTime;

    @NotNull(message = "End time is required")
    private LocalTime endTime;

    @Builder.Default
    private int graceMinutes = 15;

    @Builder.Default
    private BigDecimal halfDayThresholdHours = new BigDecimal("4.50");

    @Builder.Default
    private BigDecimal fullDayThresholdHours = new BigDecimal("8.00");

    @Builder.Default
    private int lateMarkPenaltyCount = 3;

    @Builder.Default
    private BigDecimal lateMarkPenaltyDeductionDays = new BigDecimal("0.5");
}

package com.ourhr.leave.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditLeaveRequest {

    @NotNull(message = "Employee ID is required")
    private UUID employeeId;

    @NotNull(message = "Leave type ID is required")
    private UUID leaveTypeId;

    @NotNull(message = "Days is required")
    @DecimalMin(value = "0.5", message = "Credited days must be at least 0.5")
    private BigDecimal days;

    private String note;
}

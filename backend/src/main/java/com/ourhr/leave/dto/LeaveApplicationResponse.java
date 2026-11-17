package com.ourhr.leave.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveApplicationResponse {

    private UUID id;
    private UUID companyId;
    private UUID employeeId;
    private UUID leaveTypeId;
    private String leaveCode;
    private String leaveName;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal totalDays;
    private String reason;
    private String status;
    private UUID approverId;
    private Instant actionedAt;
    private Instant createdAt;
}

package com.ourhr.leave.dto;

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
public class LeaveBalanceResponse {

    private UUID leaveTypeId;
    private String leaveCode;
    private String leaveName;
    private BigDecimal creditedDays;
    private BigDecimal consumedDays;
    private BigDecimal remainingBalance;
}

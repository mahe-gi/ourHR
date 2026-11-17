package com.ourhr.leave.dto;

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
public class LeaveTypeResponse {

    private UUID id;
    private UUID companyId;
    private String leaveName;
    private String leaveCode;
    private BigDecimal annualQuota;
    private String accrualFrequency;
    private boolean isCarryForwardAllowed;
    private BigDecimal maxCarryForwardLimit;
    private boolean isSandwichRuleEnabled;
    private Instant createdAt;
}

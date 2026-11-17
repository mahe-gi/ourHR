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
public class PunchResponse {

    private UUID id;
    private UUID companyId;
    private UUID employeeId;
    private Instant punchTimestamp;
    private String punchType;
    private String mode;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String selfieImageUrl;
    private boolean isRegularized;
    private Instant createdAt;
}

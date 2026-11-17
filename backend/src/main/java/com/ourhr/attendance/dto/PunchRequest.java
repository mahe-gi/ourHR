package com.ourhr.attendance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
public class PunchRequest {

    private UUID employeeId;

    @NotBlank(message = "Punch type is required")
    @Pattern(regexp = "^(IN|OUT)$", message = "Punch type must be IN or OUT")
    private String punchType;

    @NotBlank(message = "Mode is required")
    @Pattern(regexp = "^(WEB|MOBILE_GPS|BIOMETRIC_CSV|BIOMETRIC_API)$", message = "Mode must be WEB, MOBILE_GPS, BIOMETRIC_CSV, or BIOMETRIC_API")
    private String mode;

    private BigDecimal latitude;
    private BigDecimal longitude;
    private String selfieImageUrl;
    private Instant punchTimestamp;
}

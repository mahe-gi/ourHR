package com.ourhr.payroll.dto;

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
public class ConsultantPayoutResponse {

    private UUID id;
    private UUID companyId;
    private UUID consultantEmployeeId;
    private String consultantName;
    private String panNumber;
    private BigDecimal grossAmount;
    private String tdsSection;
    private BigDecimal tdsRate;
    private BigDecimal tdsAmount;
    private BigDecimal netPayable;
    private String status;
    private Instant createdAt;
}

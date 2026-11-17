package com.ourhr.employee.salary.dto;

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
public class PreviousEmployerIncomeResponse {

    private UUID id;
    private UUID companyId;
    private UUID employeeId;
    private String financialYear;
    private BigDecimal grossSalary;
    private BigDecimal pfDeducted;
    private BigDecimal ptDeducted;
    private BigDecimal tdsDeducted;
    private String form12bDocumentUrl;
    private boolean isVerified;
    private Instant createdAt;
}

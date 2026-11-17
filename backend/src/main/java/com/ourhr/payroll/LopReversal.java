package com.ourhr.payroll;

import com.ourhr.common.BaseTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "lop_reversals")
public class LopReversal extends BaseTenantEntity {

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(name = "original_payroll_month", nullable = false)
    private Integer originalPayrollMonth;

    @Column(name = "original_payroll_year", nullable = false)
    private Integer originalPayrollYear;

    @Column(name = "reversed_days", precision = 4, scale = 1, nullable = false)
    private BigDecimal reversedDays;

    @Column(name = "reversal_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal reversalAmount;

    @Builder.Default
    @Column(name = "status", length = 20, nullable = false)
    private String status = "PENDING";

    @Column(name = "processed_in_payroll_run_id")
    private UUID processedInPayrollRunId;

    @Column(name = "approved_by")
    private UUID approvedBy;
}

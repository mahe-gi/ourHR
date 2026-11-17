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
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "payroll_runs")
public class PayrollRun extends BaseTenantEntity {

    @Column(name = "payroll_month", nullable = false)
    private Integer payrollMonth;

    @Column(name = "payroll_year", nullable = false)
    private Integer payrollYear;

    @Builder.Default
    @Column(name = "run_status", length = 30, nullable = false)
    private String runStatus = "DRAFT";

    @Builder.Default
    @Column(name = "total_gross_pay", precision = 12, scale = 2, nullable = false)
    private BigDecimal totalGrossPay = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "total_net_pay", precision = 12, scale = 2, nullable = false)
    private BigDecimal totalNetPay = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "total_pf_liability", precision = 12, scale = 2, nullable = false)
    private BigDecimal totalPfLiability = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "total_esic_liability", precision = 12, scale = 2, nullable = false)
    private BigDecimal totalEsicLiability = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "total_pt_liability", precision = 12, scale = 2, nullable = false)
    private BigDecimal totalPtLiability = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "total_tds_liability", precision = 12, scale = 2, nullable = false)
    private BigDecimal totalTdsLiability = BigDecimal.ZERO;

    @Column(name = "bank_payout_file_url", length = 500)
    private String bankPayoutFileUrl;

    @Column(name = "ecr_txt_file_url", length = 500)
    private String ecrTxtFileUrl;

    @Column(name = "initiated_by", nullable = false)
    private UUID initiatedBy;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;
}

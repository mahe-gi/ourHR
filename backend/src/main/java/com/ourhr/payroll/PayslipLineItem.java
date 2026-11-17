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
@Table(name = "payslip_line_items")
public class PayslipLineItem extends BaseTenantEntity {

    @Column(name = "payroll_run_id", nullable = false)
    private UUID payrollRunId;

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(name = "item_category", length = 30, nullable = false)
    private String itemCategory;

    @Column(name = "item_name", length = 100, nullable = false)
    private String itemName;

    @Column(name = "item_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal itemAmount;

    @Column(name = "payslip_pdf_url", length = 500)
    private String payslipPdfUrl;
}

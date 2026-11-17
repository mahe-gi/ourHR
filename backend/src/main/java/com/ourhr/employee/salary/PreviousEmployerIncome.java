package com.ourhr.employee.salary;

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
@Table(name = "previous_employer_income")
public class PreviousEmployerIncome extends BaseTenantEntity {

    @Column(name = "employee_id", nullable = false, unique = true)
    private UUID employeeId;

    @Column(name = "financial_year", length = 9, nullable = false)
    private String financialYear;

    @Column(name = "gross_salary", precision = 12, scale = 2, nullable = false)
    private BigDecimal grossSalary;

    @Builder.Default
    @Column(name = "pf_deducted", precision = 12, scale = 2, nullable = false)
    private BigDecimal pfDeducted = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "pt_deducted", precision = 12, scale = 2, nullable = false)
    private BigDecimal ptDeducted = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "tds_deducted", precision = 12, scale = 2, nullable = false)
    private BigDecimal tdsDeducted = BigDecimal.ZERO;

    @Column(name = "form_12b_document_url", length = 500)
    private String form12bDocumentUrl;

    @Builder.Default
    @Column(name = "is_verified", nullable = false)
    private Boolean isVerified = false;
}

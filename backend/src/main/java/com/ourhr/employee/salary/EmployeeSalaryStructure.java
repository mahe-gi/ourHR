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
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "employee_salary_structures")
public class EmployeeSalaryStructure extends BaseTenantEntity {

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(name = "annual_ctc", precision = 12, scale = 2, nullable = false)
    private BigDecimal annualCtc;

    @Column(name = "monthly_gross", precision = 12, scale = 2, nullable = false)
    private BigDecimal monthlyGross;

    @Column(name = "basic_pay", precision = 12, scale = 2, nullable = false)
    private BigDecimal basicPay;

    @Builder.Default
    @Column(name = "da_pay", precision = 12, scale = 2, nullable = false)
    private BigDecimal daPay = BigDecimal.ZERO;

    @Column(name = "hra_pay", precision = 12, scale = 2, nullable = false)
    private BigDecimal hraPay;

    @Column(name = "special_allowance", precision = 12, scale = 2, nullable = false)
    private BigDecimal specialAllowance;

    @Builder.Default
    @Column(name = "pf_opt_out", nullable = false)
    private Boolean pfOptOut = false;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;
}

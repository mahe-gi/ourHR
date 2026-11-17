package com.ourhr.statutory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "lwf_rules")
public class LwfRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "state_code", length = 2, nullable = false)
    private String stateCode;

    @Column(name = "employee_contribution", precision = 12, scale = 2, nullable = false)
    private BigDecimal employeeContribution;

    @Column(name = "employer_contribution", precision = 12, scale = 2, nullable = false)
    private BigDecimal employerContribution;

    @Column(name = "deduction_frequency", length = 20, nullable = false)
    private String deductionFrequency;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "deduction_months", columnDefinition = "integer[]")
    private List<Integer> deductionMonths;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}

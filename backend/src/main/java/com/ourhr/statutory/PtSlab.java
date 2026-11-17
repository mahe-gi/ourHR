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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "pt_slabs")
public class PtSlab {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "state_code", length = 2, nullable = false)
    private String stateCode;

    @Builder.Default
    @Column(name = "gender", length = 10, nullable = false)
    private String gender = "ALL";

    @Column(name = "salary_from", precision = 12, scale = 2, nullable = false)
    private BigDecimal salaryFrom;

    @Column(name = "salary_to", precision = 12, scale = 2)
    private BigDecimal salaryTo;

    @Column(name = "tax_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal taxAmount;

    @Column(name = "special_month")
    private Integer specialMonth;

    @Column(name = "special_month_tax_amount", precision = 12, scale = 2)
    private BigDecimal specialMonthTaxAmount;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}

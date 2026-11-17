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
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tds_tax_slabs")
public class TdsTaxSlab {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "financial_year", length = 9, nullable = false)
    private String financialYear;

    @Column(name = "regime", length = 10, nullable = false)
    private String regime;

    @Column(name = "income_from", precision = 12, scale = 2, nullable = false)
    private BigDecimal incomeFrom;

    @Column(name = "income_to", precision = 12, scale = 2)
    private BigDecimal incomeTo;

    @Column(name = "tax_rate", precision = 5, scale = 2, nullable = false)
    private BigDecimal taxRate;

    @Column(name = "standard_deduction", precision = 12, scale = 2, nullable = false)
    private BigDecimal standardDeduction;

    @Column(name = "rebate_87a_limit", precision = 12, scale = 2)
    private BigDecimal rebate87aLimit;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}

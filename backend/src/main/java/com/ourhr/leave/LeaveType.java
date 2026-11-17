package com.ourhr.leave;

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

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "leave_types")
public class LeaveType extends BaseTenantEntity {

    @Column(name = "leave_name", length = 100, nullable = false)
    private String leaveName;

    @Column(name = "leave_code", length = 20, nullable = false)
    private String leaveCode;

    @Column(name = "annual_quota", precision = 4, scale = 1, nullable = false)
    private BigDecimal annualQuota;

    @Column(name = "accrual_frequency", length = 20, nullable = false)
    private String accrualFrequency;

    @Builder.Default
    @Column(name = "is_carry_forward_allowed", nullable = false)
    private Boolean isCarryForwardAllowed = true;

    @Builder.Default
    @Column(name = "max_carry_forward_limit", precision = 4, scale = 1, nullable = false)
    private BigDecimal maxCarryForwardLimit = new BigDecimal("30.0");

    @Builder.Default
    @Column(name = "is_sandwich_rule_enabled", nullable = false)
    private Boolean isSandwichRuleEnabled = false;
}

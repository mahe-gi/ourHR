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

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "salary_components")
public class SalaryComponent extends BaseTenantEntity {

    @Column(name = "component_name", length = 100, nullable = false)
    private String componentName;

    @Column(name = "component_code", length = 50, nullable = false)
    private String componentCode;

    @Column(name = "component_type", length = 20, nullable = false)
    private String componentType;

    @Builder.Default
    @Column(name = "is_statutory_wage", nullable = false)
    private Boolean isStatutoryWage = false;

    @Builder.Default
    @Column(name = "is_taxable", nullable = false)
    private Boolean isTaxable = true;
}

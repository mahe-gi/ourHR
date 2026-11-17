package com.ourhr.company;

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
@Table(name = "branches")
public class Branch extends BaseTenantEntity {

    @Column(name = "branch_name", length = 150, nullable = false)
    private String branchName;

    @Column(name = "state_code", length = 2, nullable = false)
    private String stateCode;

    @Column(name = "city", length = 100, nullable = false)
    private String city;

    @Column(name = "address_line", nullable = false)
    private String addressLine;

    @Column(name = "pincode", length = 10, nullable = false)
    private String pincode;

    @Builder.Default
    @Column(name = "is_headquarters", nullable = false)
    private Boolean isHeadquarters = false;
}

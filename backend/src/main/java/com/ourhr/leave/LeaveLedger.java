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
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "leave_ledgers")
public class LeaveLedger extends BaseTenantEntity {

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(name = "leave_type_id", nullable = false)
    private UUID leaveTypeId;

    @Column(name = "transaction_type", length = 20, nullable = false)
    private String transactionType;

    @Column(name = "days", precision = 4, scale = 1, nullable = false)
    private BigDecimal days;

    @Column(name = "leave_application_id")
    private UUID leaveApplicationId;

    @Column(name = "reference_note", length = 255)
    private String referenceNote;

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;
}

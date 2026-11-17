package com.ourhr.payroll;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PayslipLineItemRepository extends JpaRepository<PayslipLineItem, UUID> {

    List<PayslipLineItem> findByCompanyIdAndPayrollRunId(UUID companyId, UUID payrollRunId);

    List<PayslipLineItem> findByCompanyIdAndPayrollRunIdAndEmployeeId(
            UUID companyId,
            UUID payrollRunId,
            UUID employeeId
    );

    @Modifying
    @Query("DELETE FROM PayslipLineItem p WHERE p.payrollRunId = :payrollRunId AND p.companyId = :companyId")
    void deleteByPayrollRunIdAndCompanyId(
            @Param("payrollRunId") UUID payrollRunId,
            @Param("companyId") UUID companyId
    );
}

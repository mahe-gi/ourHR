package com.ourhr.leave;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface LeaveLedgerRepository extends JpaRepository<LeaveLedger, UUID> {

    @Query("SELECT COALESCE(SUM(l.days), 0) FROM LeaveLedger l WHERE l.companyId = :companyId AND l.employeeId = :employeeId AND l.leaveTypeId = :leaveTypeId")
    BigDecimal getBalance(
            @Param("companyId") UUID companyId,
            @Param("employeeId") UUID employeeId,
            @Param("leaveTypeId") UUID leaveTypeId
    );

    List<LeaveLedger> findByCompanyIdAndEmployeeIdOrderByCreatedAtDesc(UUID companyId, UUID employeeId);

    List<LeaveLedger> findByCompanyIdAndEmployeeIdAndLeaveTypeIdOrderByCreatedAtDesc(
            UUID companyId,
            UUID employeeId,
            UUID leaveTypeId
    );

    List<LeaveLedger> findByCompanyIdAndLeaveApplicationId(UUID companyId, UUID leaveApplicationId);
}

package com.ourhr.payroll;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LopReversalRepository extends JpaRepository<LopReversal, UUID> {

    List<LopReversal> findByCompanyIdAndStatus(UUID companyId, String status);

    List<LopReversal> findByCompanyIdAndEmployeeId(UUID companyId, UUID employeeId);

    List<LopReversal> findByCompanyId(UUID companyId);

    Optional<LopReversal> findByCompanyIdAndId(UUID companyId, UUID id);

    List<LopReversal> findByCompanyIdAndProcessedInPayrollRunId(UUID companyId, UUID processedInPayrollRunId);

    List<LopReversal> findByCompanyIdAndEmployeeIdAndProcessedInPayrollRunId(
            UUID companyId,
            UUID employeeId,
            UUID processedInPayrollRunId
    );
}

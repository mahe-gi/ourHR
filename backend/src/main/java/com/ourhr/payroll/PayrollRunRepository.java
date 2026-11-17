package com.ourhr.payroll;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PayrollRunRepository extends JpaRepository<PayrollRun, UUID> {

    Optional<PayrollRun> findByCompanyIdAndPayrollMonthAndPayrollYear(
            UUID companyId,
            Integer payrollMonth,
            Integer payrollYear
    );

    List<PayrollRun> findByCompanyIdOrderByPayrollYearDescPayrollMonthDesc(UUID companyId);

    Optional<PayrollRun> findByCompanyIdAndId(UUID companyId, UUID id);

    boolean existsByCompanyIdAndPayrollMonthAndPayrollYear(
            UUID companyId,
            Integer payrollMonth,
            Integer payrollYear
    );
}

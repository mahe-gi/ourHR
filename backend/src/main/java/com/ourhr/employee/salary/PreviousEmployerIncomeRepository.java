package com.ourhr.employee.salary;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PreviousEmployerIncomeRepository extends JpaRepository<PreviousEmployerIncome, UUID> {

    Optional<PreviousEmployerIncome> findByCompanyIdAndEmployeeIdAndFinancialYear(UUID companyId, UUID employeeId, String financialYear);

    Optional<PreviousEmployerIncome> findByCompanyIdAndEmployeeId(UUID companyId, UUID employeeId);

    Optional<PreviousEmployerIncome> findByEmployeeId(UUID employeeId);

    Optional<PreviousEmployerIncome> findByEmployeeIdAndFinancialYear(UUID employeeId, String financialYear);

    List<PreviousEmployerIncome> findByCompanyIdAndFinancialYear(UUID companyId, String financialYear);
}

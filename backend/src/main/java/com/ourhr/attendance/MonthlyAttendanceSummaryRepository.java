package com.ourhr.attendance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MonthlyAttendanceSummaryRepository extends JpaRepository<MonthlyAttendanceSummary, UUID> {

    Optional<MonthlyAttendanceSummary> findByCompanyIdAndEmployeeIdAndPayrollMonthAndPayrollYear(
            UUID companyId,
            UUID employeeId,
            Integer payrollMonth,
            Integer payrollYear
    );

    List<MonthlyAttendanceSummary> findByCompanyIdAndPayrollMonthAndPayrollYear(
            UUID companyId,
            Integer payrollMonth,
            Integer payrollYear
    );

    List<MonthlyAttendanceSummary> findByCompanyIdAndEmployeeId(UUID companyId, UUID employeeId);
}

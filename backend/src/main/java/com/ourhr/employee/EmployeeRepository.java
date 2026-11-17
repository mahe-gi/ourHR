package com.ourhr.employee;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, UUID> {

    Optional<Employee> findByCompanyIdAndEmployeeCode(UUID companyId, String employeeCode);

    Optional<Employee> findByCompanyIdAndId(UUID companyId, UUID id);

    Optional<Employee> findByIdAndCompanyId(UUID id, UUID companyId);

    List<Employee> findByCompanyIdAndBranchId(UUID companyId, UUID branchId);

    boolean existsByCompanyIdAndEmployeeCode(UUID companyId, String employeeCode);

    List<Employee> findByCompanyId(UUID companyId);

    List<Employee> findByCompanyIdAndReportingManagerId(UUID companyId, UUID reportingManagerId);

    Optional<Employee> findByCompanyIdAndUserId(UUID companyId, UUID userId);

    Optional<Employee> findByUserId(UUID userId);
}

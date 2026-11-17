package com.ourhr.employee.salary;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmployeeSalaryStructureRepository extends JpaRepository<EmployeeSalaryStructure, UUID> {

    @Query("SELECT s FROM EmployeeSalaryStructure s " +
           "WHERE s.companyId = :companyId " +
           "  AND s.employeeId = :employeeId " +
           "  AND s.effectiveFrom <= :onDate " +
           "  AND (s.effectiveTo IS NULL OR s.effectiveTo >= :onDate) " +
           "ORDER BY s.effectiveFrom DESC")
    Optional<EmployeeSalaryStructure> findActiveStructure(
            @Param("companyId") UUID companyId,
            @Param("employeeId") UUID employeeId,
            @Param("onDate") LocalDate onDate
    );

    @Query("SELECT s FROM EmployeeSalaryStructure s " +
           "WHERE s.employeeId = :employeeId " +
           "  AND s.effectiveFrom <= :onDate " +
           "  AND (s.effectiveTo IS NULL OR s.effectiveTo >= :onDate) " +
           "ORDER BY s.effectiveFrom DESC")
    Optional<EmployeeSalaryStructure> findActiveStructureByEmployeeId(
            @Param("employeeId") UUID employeeId,
            @Param("onDate") LocalDate onDate
    );

    List<EmployeeSalaryStructure> findByEmployeeIdOrderByEffectiveFromDesc(UUID employeeId);

    List<EmployeeSalaryStructure> findByCompanyIdAndEmployeeIdOrderByEffectiveFromDesc(UUID companyId, UUID employeeId);

    Optional<EmployeeSalaryStructure> findByCompanyIdAndEmployeeIdAndEffectiveToIsNull(UUID companyId, UUID employeeId);
}

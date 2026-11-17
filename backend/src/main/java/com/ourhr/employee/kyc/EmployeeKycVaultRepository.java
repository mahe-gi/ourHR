package com.ourhr.employee.kyc;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmployeeKycVaultRepository extends JpaRepository<EmployeeKycVault, UUID> {

    Optional<EmployeeKycVault> findByCompanyIdAndEmployeeId(UUID companyId, UUID employeeId);

    Optional<EmployeeKycVault> findByEmployeeId(UUID employeeId);

    boolean existsByCompanyIdAndPanNumber(UUID companyId, String panNumber);

    List<EmployeeKycVault> findByCompanyId(UUID companyId);
}

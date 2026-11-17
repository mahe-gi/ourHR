package com.ourhr.payroll;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SalaryComponentRepository extends JpaRepository<SalaryComponent, UUID> {

    List<SalaryComponent> findByCompanyId(UUID companyId);

    Optional<SalaryComponent> findByCompanyIdAndComponentCode(UUID companyId, String componentCode);
}

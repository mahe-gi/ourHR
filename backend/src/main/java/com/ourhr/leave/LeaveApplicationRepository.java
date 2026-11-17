package com.ourhr.leave;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LeaveApplicationRepository extends JpaRepository<LeaveApplication, UUID> {

    List<LeaveApplication> findByCompanyIdAndEmployeeId(UUID companyId, UUID employeeId);

    List<LeaveApplication> findByCompanyIdAndStatus(UUID companyId, String status);

    List<LeaveApplication> findByCompanyIdAndEmployeeIdAndStatus(UUID companyId, UUID employeeId, String status);

    Optional<LeaveApplication> findByIdAndCompanyId(UUID id, UUID companyId);
}

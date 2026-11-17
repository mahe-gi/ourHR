package com.ourhr.leave;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LeaveTypeRepository extends JpaRepository<LeaveType, UUID> {

    Optional<LeaveType> findByCompanyIdAndLeaveCode(UUID companyId, String leaveCode);

    List<LeaveType> findByCompanyId(UUID companyId);

    Optional<LeaveType> findByIdAndCompanyId(UUID id, UUID companyId);
}

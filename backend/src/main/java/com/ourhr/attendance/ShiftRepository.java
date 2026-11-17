package com.ourhr.attendance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShiftRepository extends JpaRepository<Shift, UUID> {

    List<Shift> findByCompanyId(UUID companyId);

    Optional<Shift> findByCompanyIdAndShiftName(UUID companyId, String shiftName);

    Optional<Shift> findByIdAndCompanyId(UUID id, UUID companyId);
}

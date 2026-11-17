package com.ourhr.attendance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface AttendanceLogRepository extends JpaRepository<AttendanceLog, UUID> {

    List<AttendanceLog> findByCompanyIdAndEmployeeIdAndPunchTimestampBetween(
            UUID companyId,
            UUID employeeId,
            Instant start,
            Instant end
    );

    List<AttendanceLog> findByEmployeeIdOrderByPunchTimestampDesc(UUID employeeId);

    List<AttendanceLog> findByCompanyIdAndEmployeeIdOrderByPunchTimestampDesc(UUID companyId, UUID employeeId);

    List<AttendanceLog> findByCompanyIdAndPunchTimestampBetween(UUID companyId, Instant start, Instant end);
}

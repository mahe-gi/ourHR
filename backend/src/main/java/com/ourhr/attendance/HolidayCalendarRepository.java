package com.ourhr.attendance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HolidayCalendarRepository extends JpaRepository<HolidayCalendar, UUID> {

    @Query("SELECT h FROM HolidayCalendar h WHERE h.companyId = :companyId " +
           "AND (h.stateCode = :stateCode OR h.stateCode = 'ALL') " +
           "AND h.holidayDate BETWEEN :startDate AND :endDate ORDER BY h.holidayDate ASC")
    List<HolidayCalendar> findHolidaysBetween(
            @Param("companyId") UUID companyId,
            @Param("stateCode") String stateCode,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT h FROM HolidayCalendar h WHERE h.companyId = :companyId " +
           "AND h.holidayDate BETWEEN :startDate AND :endDate ORDER BY h.holidayDate ASC")
    List<HolidayCalendar> findHolidaysBetween(
            @Param("companyId") UUID companyId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    Optional<HolidayCalendar> findByCompanyIdAndStateCodeAndHolidayDate(
            UUID companyId,
            String stateCode,
            LocalDate holidayDate
    );

    List<HolidayCalendar> findByCompanyId(UUID companyId);

    List<HolidayCalendar> findByCompanyIdAndStateCode(UUID companyId, String stateCode);
}

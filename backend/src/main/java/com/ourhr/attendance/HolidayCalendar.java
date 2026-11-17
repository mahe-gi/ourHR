package com.ourhr.attendance;

import com.ourhr.common.BaseTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "holiday_calendars")
public class HolidayCalendar extends BaseTenantEntity {

    @Column(name = "state_code", length = 2, nullable = false)
    private String stateCode;

    @Column(name = "holiday_name", length = 150, nullable = false)
    private String holidayName;

    @Column(name = "holiday_date", nullable = false)
    private LocalDate holidayDate;

    @Builder.Default
    @Column(name = "is_restricted", nullable = false)
    private Boolean isRestricted = false;
}

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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "attendance_logs")
public class AttendanceLog extends BaseTenantEntity {

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(name = "punch_timestamp", nullable = false)
    private Instant punchTimestamp;

    @Column(name = "punch_type", length = 10, nullable = false)
    private String punchType;

    @Column(name = "mode", length = 20, nullable = false)
    private String mode;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "latitude", precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 11, scale = 8)
    private BigDecimal longitude;

    @Column(name = "selfie_image_url", length = 500)
    private String selfieImageUrl;

    @Builder.Default
    @Column(name = "is_regularized", nullable = false)
    private Boolean isRegularized = false;
}

package com.ourhr.jobs;

import com.ourhr.common.BaseTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "batch_jobs")
public class BatchJob extends BaseTenantEntity {

    @Column(name = "job_type", length = 50, nullable = false)
    private String jobType;

    @Builder.Default
    @Column(name = "status", length = 20, nullable = false)
    private String status = "PENDING"; // PENDING, IN_PROGRESS, COMPLETED, FAILED

    @Builder.Default
    @Column(name = "total_items", nullable = false)
    private Integer totalItems = 0;

    @Builder.Default
    @Column(name = "processed_items", nullable = false)
    private Integer processedItems = 0;

    @Builder.Default
    @Column(name = "failed_items", nullable = false)
    private Integer failedItems = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "job_payload", columnDefinition = "jsonb")
    private String jobPayload;

    @Column(name = "error_summary")
    private String errorSummary;

    @Column(name = "initiated_by", nullable = false)
    private UUID initiatedBy;
}

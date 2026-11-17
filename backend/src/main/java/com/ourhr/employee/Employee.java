package com.ourhr.employee;

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
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "employees")
public class Employee extends BaseTenantEntity {

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(name = "user_id", unique = true)
    private UUID userId;

    @Column(name = "employee_code", length = 50, nullable = false)
    private String employeeCode;

    @Column(name = "first_name", length = 100, nullable = false)
    private String firstName;

    @Column(name = "last_name", length = 100, nullable = false)
    private String lastName;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Column(name = "gender", length = 10, nullable = false)
    private String gender;

    @Column(name = "date_of_joining", nullable = false)
    private LocalDate dateOfJoining;

    @Builder.Default
    @Column(name = "employment_status", length = 20, nullable = false)
    private String employmentStatus = "ACTIVE";

    @Builder.Default
    @Column(name = "worker_type", length = 30, nullable = false)
    private String workerType = "REGULAR";

    @Column(name = "reporting_manager_id")
    private UUID reportingManagerId;

    @Column(name = "department", length = 100)
    private String department;

    @Column(name = "designation", length = 100)
    private String designation;
}

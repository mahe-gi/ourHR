package com.ourhr.employee.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeResponse {

    private UUID id;
    private UUID companyId;
    private UUID branchId;
    private UUID userId;
    private String employeeCode;
    private String firstName;
    private String lastName;
    private String officialEmail;
    private LocalDate dateOfBirth;
    private String gender;
    private LocalDate dateOfJoining;
    private String employmentStatus;
    private String workerType;
    private UUID reportingManagerId;
    private String department;
    private String designation;
    private Instant createdAt;
    private Instant updatedAt;
}

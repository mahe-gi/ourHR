package com.ourhr.employee.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateEmployeeRequest {

    private UUID branchId;

    @Size(max = 100, message = "First name cannot exceed 100 characters")
    private String firstName;

    @Size(max = 100, message = "Last name cannot exceed 100 characters")
    private String lastName;

    private LocalDate dateOfBirth;

    @Size(max = 10, message = "Gender cannot exceed 10 characters")
    private String gender;

    private UUID reportingManagerId;

    @Size(max = 100, message = "Department cannot exceed 100 characters")
    private String department;

    @Size(max = 100, message = "Designation cannot exceed 100 characters")
    private String designation;

    @Size(max = 20, message = "Employment status cannot exceed 20 characters")
    private String employmentStatus;

    @Size(max = 30, message = "Worker type cannot exceed 30 characters")
    private String workerType;
}

package com.ourhr.employee.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class CreateEmployeeRequest {

    @NotBlank(message = "Employee code is required")
    @Size(max = 50, message = "Employee code cannot exceed 50 characters")
    private String employeeCode;

    @NotNull(message = "Branch ID is required")
    private UUID branchId;

    @NotBlank(message = "First name is required")
    @Size(max = 100, message = "First name cannot exceed 100 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 100, message = "Last name cannot exceed 100 characters")
    private String lastName;

    @Email(message = "Invalid official email address")
    private String officialEmail;

    @NotNull(message = "Date of birth is required")
    private LocalDate dateOfBirth;

    @NotBlank(message = "Gender is required")
    @Size(max = 10, message = "Gender cannot exceed 10 characters")
    private String gender;

    @NotNull(message = "Date of joining is required")
    private LocalDate dateOfJoining;

    @Builder.Default
    private String workerType = "REGULAR";

    private UUID reportingManagerId;

    @Size(max = 100, message = "Department cannot exceed 100 characters")
    private String department;

    @Size(max = 100, message = "Designation cannot exceed 100 characters")
    private String designation;
}

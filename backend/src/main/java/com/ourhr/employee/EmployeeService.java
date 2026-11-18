package com.ourhr.employee;

import com.ourhr.auth.User;
import com.ourhr.auth.UserRepository;
import com.ourhr.auth.UserRole;
import com.ourhr.common.ResourceNotFoundException;
import com.ourhr.common.TenantContext;
import com.ourhr.company.BranchRepository;
import com.ourhr.employee.dto.CreateEmployeeRequest;
import com.ourhr.employee.dto.EmployeeResponse;
import com.ourhr.employee.dto.UpdateEmployeeRequest;
import com.ourhr.security.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final BranchRepository branchRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public EmployeeResponse createEmployee(CreateEmployeeRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant context is required");
        }

        branchRepository.findByIdAndCompanyId(request.getBranchId(), tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Branch not found or does not belong to company: " + request.getBranchId()));

        if (employeeRepository.existsByCompanyIdAndEmployeeCode(tenantId, request.getEmployeeCode())) {
            throw new IllegalArgumentException("Employee with code " + request.getEmployeeCode() + " already exists");
        }

        if (request.getReportingManagerId() != null) {
            employeeRepository.findByIdAndCompanyId(request.getReportingManagerId(), tenantId)
                    .orElseThrow(() -> new IllegalArgumentException("Reporting manager not found in company: " + request.getReportingManagerId()));
        }

        UUID linkedUserId = null;
        if (request.getOfficialEmail() != null && !request.getOfficialEmail().isBlank()) {
            String email = request.getOfficialEmail().trim().toLowerCase();
            var existingUser = userRepository.findByEmail(email);
            if (existingUser.isPresent()) {
                User user = existingUser.get();
                if (!tenantId.equals(user.getCompanyId())) {
                    throw new IllegalArgumentException("User with email " + email + " already exists under another company");
                }
                linkedUserId = user.getId();
            } else {
                String initialPassword = "Welcome@1234";
                User newUser = User.builder()
                        .email(email)
                        .passwordHash(passwordEncoder.encode(initialPassword))
                        .fullName(request.getFirstName().trim() + " " + request.getLastName().trim())
                        .isActive(true)
                        .build();
                newUser.setCompanyId(tenantId);
                newUser.addRole(UserRole.builder().roleName(Role.ROLE_EMPLOYEE).build());
                User savedUser = userRepository.save(newUser);
                linkedUserId = savedUser.getId();
            }
        }

        Employee employee = Employee.builder()
                .branchId(request.getBranchId())
                .userId(linkedUserId)
                .employeeCode(request.getEmployeeCode().trim())
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .dateOfBirth(request.getDateOfBirth())
                .gender(request.getGender().trim().toUpperCase())
                .dateOfJoining(request.getDateOfJoining())
                .workerType(request.getWorkerType() != null && !request.getWorkerType().isBlank() ? request.getWorkerType() : "REGULAR")
                .reportingManagerId(request.getReportingManagerId())
                .department(request.getDepartment())
                .designation(request.getDesignation())
                .build();
        employee.setCompanyId(tenantId);

        Employee saved = employeeRepository.save(employee);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public EmployeeResponse getEmployeeById(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant context is required");
        }

        Employee employee = employeeRepository.findByIdAndCompanyId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));

        return toResponse(employee);
    }

    @Transactional(readOnly = true)
    public List<EmployeeResponse> getAllEmployees() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant context is required");
        }

        return employeeRepository.findByCompanyId(tenantId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public EmployeeResponse updateEmployee(UUID id, UpdateEmployeeRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant context is required");
        }

        Employee employee = employeeRepository.findByIdAndCompanyId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));

        if (request.getBranchId() != null) {
            branchRepository.findByIdAndCompanyId(request.getBranchId(), tenantId)
                    .orElseThrow(() -> new IllegalArgumentException("Branch not found or does not belong to company: " + request.getBranchId()));
            employee.setBranchId(request.getBranchId());
        }

        if (request.getFirstName() != null && !request.getFirstName().isBlank()) {
            employee.setFirstName(request.getFirstName().trim());
        }

        if (request.getLastName() != null && !request.getLastName().isBlank()) {
            employee.setLastName(request.getLastName().trim());
        }

        if (request.getDateOfBirth() != null) {
            employee.setDateOfBirth(request.getDateOfBirth());
        }

        if (request.getGender() != null && !request.getGender().isBlank()) {
            employee.setGender(request.getGender().trim().toUpperCase());
        }

        if (request.getReportingManagerId() != null) {
            if (id.equals(request.getReportingManagerId())) {
                throw new IllegalArgumentException("Employee cannot be their own reporting manager");
            }
            employeeRepository.findByIdAndCompanyId(request.getReportingManagerId(), tenantId)
                    .orElseThrow(() -> new IllegalArgumentException("Reporting manager not found in company: " + request.getReportingManagerId()));
            employee.setReportingManagerId(request.getReportingManagerId());
        }

        if (request.getDepartment() != null) {
            employee.setDepartment(request.getDepartment());
        }

        if (request.getDesignation() != null) {
            employee.setDesignation(request.getDesignation());
        }

        if (request.getEmploymentStatus() != null) {
            employee.setEmploymentStatus(request.getEmploymentStatus());
        }

        if (request.getWorkerType() != null) {
            employee.setWorkerType(request.getWorkerType());
        }

        Employee updated = employeeRepository.save(employee);
        return toResponse(updated);
    }

    public EmployeeResponse toResponse(Employee employee) {
        String officialEmail = null;
        if (employee.getUserId() != null) {
            officialEmail = userRepository.findById(employee.getUserId())
                    .map(User::getEmail)
                    .orElse(null);
        }

        return EmployeeResponse.builder()
                .id(employee.getId())
                .companyId(employee.getCompanyId())
                .branchId(employee.getBranchId())
                .userId(employee.getUserId())
                .employeeCode(employee.getEmployeeCode())
                .firstName(employee.getFirstName())
                .lastName(employee.getLastName())
                .officialEmail(officialEmail)
                .dateOfBirth(employee.getDateOfBirth())
                .gender(employee.getGender())
                .dateOfJoining(employee.getDateOfJoining())
                .employmentStatus(employee.getEmploymentStatus())
                .workerType(employee.getWorkerType())
                .reportingManagerId(employee.getReportingManagerId())
                .department(employee.getDepartment())
                .designation(employee.getDesignation())
                .createdAt(employee.getCreatedAt())
                .updatedAt(employee.getUpdatedAt())
                .build();
    }
}

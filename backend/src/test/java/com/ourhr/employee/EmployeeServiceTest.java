package com.ourhr.employee;

import com.ourhr.auth.User;
import com.ourhr.auth.UserRepository;
import com.ourhr.common.ResourceNotFoundException;
import com.ourhr.common.TenantContext;
import com.ourhr.company.Branch;
import com.ourhr.company.BranchRepository;
import com.ourhr.employee.dto.CreateEmployeeRequest;
import com.ourhr.employee.dto.EmployeeResponse;
import com.ourhr.employee.dto.UpdateEmployeeRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private BranchRepository branchRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private EmployeeService employeeService;

    private UUID tenantId;
    private UUID branchId;
    private UUID employeeId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        branchId = UUID.randomUUID();
        employeeId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Create employee successfully creates employee and linked user account")
    void shouldCreateEmployeeAndLinkedUser() {
        Branch branch = Branch.builder().branchName("Headquarters").stateCode("MH").build();
        branch.setId(branchId);
        branch.setCompanyId(tenantId);

        when(branchRepository.findByIdAndCompanyId(eq(branchId), eq(tenantId)))
                .thenReturn(Optional.of(branch));
        when(employeeRepository.existsByCompanyIdAndEmployeeCode(eq(tenantId), eq("EMP-001")))
                .thenReturn(false);
        when(userRepository.findByEmail(eq("john.doe@company.in"))).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("hashed-password");

        UUID generatedUserId = UUID.randomUUID();
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(generatedUserId);
            return u;
        });

        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> {
            Employee emp = invocation.getArgument(0);
            emp.setId(employeeId);
            return emp;
        });

        CreateEmployeeRequest request = CreateEmployeeRequest.builder()
                .branchId(branchId)
                .employeeCode("EMP-001")
                .firstName("John")
                .lastName("Doe")
                .officialEmail("john.doe@company.in")
                .dateOfBirth(LocalDate.of(1990, 5, 15))
                .gender("MALE")
                .dateOfJoining(LocalDate.of(2025, 1, 10))
                .workerType("REGULAR")
                .department("Engineering")
                .designation("Senior Developer")
                .build();

        EmployeeResponse response = employeeService.createEmployee(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(employeeId);
        assertThat(response.getEmployeeCode()).isEqualTo("EMP-001");
        assertThat(response.getFirstName()).isEqualTo("John");
        assertThat(response.getLastName()).isEqualTo("Doe");

        ArgumentCaptor<Employee> empCaptor = ArgumentCaptor.forClass(Employee.class);
        verify(employeeRepository).save(empCaptor.capture());
        Employee saved = empCaptor.getValue();
        assertThat(saved.getCompanyId()).isEqualTo(tenantId);
        assertThat(saved.getBranchId()).isEqualTo(branchId);
        assertThat(saved.getUserId()).isEqualTo(generatedUserId);
    }

    @Test
    @DisplayName("Throws IllegalArgumentException when duplicate employeeCode exists in company")
    void shouldRejectDuplicateEmployeeCodeInSameCompany() {
        Branch branch = Branch.builder().build();
        branch.setId(branchId);
        branch.setCompanyId(tenantId);

        when(branchRepository.findByIdAndCompanyId(eq(branchId), eq(tenantId)))
                .thenReturn(Optional.of(branch));
        when(employeeRepository.existsByCompanyIdAndEmployeeCode(eq(tenantId), eq("EMP-DUP")))
                .thenReturn(true);

        CreateEmployeeRequest request = CreateEmployeeRequest.builder()
                .branchId(branchId)
                .employeeCode("EMP-DUP")
                .firstName("Jane")
                .lastName("Doe")
                .gender("FEMALE")
                .dateOfBirth(LocalDate.of(1992, 1, 1))
                .dateOfJoining(LocalDate.of(2025, 2, 1))
                .build();

        assertThatThrownBy(() -> employeeService.createEmployee(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Employee with code EMP-DUP already exists");

        verify(employeeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Throws IllegalArgumentException when branch does not belong to company")
    void shouldRejectBranchFromAnotherCompany() {
        when(branchRepository.findByIdAndCompanyId(eq(branchId), eq(tenantId)))
                .thenReturn(Optional.empty());

        CreateEmployeeRequest request = CreateEmployeeRequest.builder()
                .branchId(branchId)
                .employeeCode("EMP-002")
                .build();

        assertThatThrownBy(() -> employeeService.createEmployee(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Branch not found or does not belong to company: " + branchId);
    }

    @Test
    @DisplayName("Throws IllegalArgumentException when user email exists under another company")
    void shouldRejectEmailBelongingToAnotherCompany() {
        Branch branch = Branch.builder().build();
        branch.setId(branchId);
        branch.setCompanyId(tenantId);

        when(branchRepository.findByIdAndCompanyId(eq(branchId), eq(tenantId)))
                .thenReturn(Optional.of(branch));
        when(employeeRepository.existsByCompanyIdAndEmployeeCode(eq(tenantId), eq("EMP-003")))
                .thenReturn(false);

        User crossTenantUser = User.builder().email("external@othercompany.in").build();
        crossTenantUser.setCompanyId(UUID.randomUUID()); // different company

        when(userRepository.findByEmail(eq("external@othercompany.in")))
                .thenReturn(Optional.of(crossTenantUser));

        CreateEmployeeRequest request = CreateEmployeeRequest.builder()
                .branchId(branchId)
                .employeeCode("EMP-003")
                .firstName("External")
                .lastName("User")
                .officialEmail("external@othercompany.in")
                .gender("MALE")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .dateOfJoining(LocalDate.of(2025, 1, 1))
                .build();

        assertThatThrownBy(() -> employeeService.createEmployee(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists under another company");
    }

    @Test
    @DisplayName("Multi-tenant isolation: getEmployeeById filters strictly by tenantId")
    void shouldGetEmployeeByIdScopedToTenant() {
        Employee employee = Employee.builder()
                .employeeCode("EMP-004")
                .firstName("Arun")
                .lastName("Kumar")
                .build();
        employee.setId(employeeId);
        employee.setCompanyId(tenantId);

        when(employeeRepository.findByIdAndCompanyId(eq(employeeId), eq(tenantId)))
                .thenReturn(Optional.of(employee));

        EmployeeResponse response = employeeService.getEmployeeById(employeeId);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(employeeId);
        assertThat(response.getEmployeeCode()).isEqualTo("EMP-004");
    }

    @Test
    @DisplayName("getEmployeeById throws ResourceNotFoundException when employee belongs to another tenant")
    void shouldThrowWhenEmployeeNotFoundForCurrentTenant() {
        when(employeeRepository.findByIdAndCompanyId(eq(employeeId), eq(tenantId)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.getEmployeeById(employeeId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Employee not found with id: " + employeeId);
    }

    @Test
    @DisplayName("getAllEmployees returns only employees belonging to tenant")
    void shouldGetAllEmployeesForTenant() {
        Employee emp1 = Employee.builder().employeeCode("EMP-01").firstName("A").lastName("B").build();
        emp1.setId(UUID.randomUUID());
        emp1.setCompanyId(tenantId);

        Employee emp2 = Employee.builder().employeeCode("EMP-02").firstName("C").lastName("D").build();
        emp2.setId(UUID.randomUUID());
        emp2.setCompanyId(tenantId);

        when(employeeRepository.findByCompanyId(eq(tenantId)))
                .thenReturn(List.of(emp1, emp2));

        List<EmployeeResponse> responses = employeeService.getAllEmployees();

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getEmployeeCode()).isEqualTo("EMP-01");
        assertThat(responses.get(1).getEmployeeCode()).isEqualTo("EMP-02");
    }

    @Test
    @DisplayName("Update employee rejects assigning employee as their own reporting manager")
    void shouldRejectSelfReportingManager() {
        Employee employee = Employee.builder().build();
        employee.setId(employeeId);
        employee.setCompanyId(tenantId);

        when(employeeRepository.findByIdAndCompanyId(eq(employeeId), eq(tenantId)))
                .thenReturn(Optional.of(employee));

        UpdateEmployeeRequest request = UpdateEmployeeRequest.builder()
                .reportingManagerId(employeeId)
                .build();

        assertThatThrownBy(() -> employeeService.updateEmployee(employeeId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Employee cannot be their own reporting manager");
    }

    @Test
    @DisplayName("Update employee successfully updates fields")
    void shouldUpdateEmployeeSuccessfully() {
        Employee employee = Employee.builder()
                .employeeCode("EMP-005")
                .firstName("OldFirst")
                .lastName("OldLast")
                .department("Sales")
                .designation("Associate")
                .build();
        employee.setId(employeeId);
        employee.setCompanyId(tenantId);

        when(employeeRepository.findByIdAndCompanyId(eq(employeeId), eq(tenantId)))
                .thenReturn(Optional.of(employee));
        when(employeeRepository.save(any(Employee.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UpdateEmployeeRequest request = UpdateEmployeeRequest.builder()
                .firstName("NewFirst")
                .lastName("NewLast")
                .department("Marketing")
                .designation("Manager")
                .build();

        EmployeeResponse response = employeeService.updateEmployee(employeeId, request);

        assertThat(response.getFirstName()).isEqualTo("NewFirst");
        assertThat(response.getLastName()).isEqualTo("NewLast");
        assertThat(response.getDepartment()).isEqualTo("Marketing");
        assertThat(response.getDesignation()).isEqualTo("Manager");
    }
}

package com.ourhr.employee.salary;

import com.ourhr.common.ResourceNotFoundException;
import com.ourhr.common.TenantContext;
import com.ourhr.employee.Employee;
import com.ourhr.employee.EmployeeRepository;
import com.ourhr.employee.salary.dto.SalaryOfferRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SalaryStructureServiceTest {

    @Mock
    private EmployeeSalaryStructureRepository salaryStructureRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private BasicWageValidator basicWageValidator;

    @InjectMocks
    private SalaryStructureService salaryStructureService;

    private UUID tenantId;
    private UUID employeeId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        employeeId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Create salary structure deactivates previous active structure and persists new one")
    void shouldCreateSalaryStructureAndDeactivatePrevious() {
        Employee employee = Employee.builder().build();
        employee.setId(employeeId);
        employee.setCompanyId(tenantId);

        when(employeeRepository.findByIdAndCompanyId(eq(employeeId), eq(tenantId)))
                .thenReturn(Optional.of(employee));

        EmployeeSalaryStructure previousActive = EmployeeSalaryStructure.builder()
                .employeeId(employeeId)
                .monthlyGross(new BigDecimal("80000.00"))
                .basicPay(new BigDecimal("40000.00"))
                .effectiveFrom(LocalDate.of(2025, 4, 1))
                .effectiveTo(null)
                .build();
        previousActive.setId(UUID.randomUUID());
        previousActive.setCompanyId(tenantId);

        when(salaryStructureRepository.findByCompanyIdAndEmployeeIdAndEffectiveToIsNull(eq(tenantId), eq(employeeId)))
                .thenReturn(Optional.of(previousActive));

        when(salaryStructureRepository.save(any(EmployeeSalaryStructure.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LocalDate newEffectiveDate = LocalDate.of(2026, 4, 1);
        SalaryOfferRequest request = SalaryOfferRequest.builder()
                .employeeId(employeeId)
                .annualCtc(new BigDecimal("1200000.00"))
                .monthlyGross(new BigDecimal("100000.00"))
                .basicPay(new BigDecimal("50000.00"))
                .daPay(BigDecimal.ZERO)
                .hraPay(new BigDecimal("25000.00"))
                .specialAllowance(new BigDecimal("25000.00"))
                .effectiveFrom(newEffectiveDate)
                .build();

        EmployeeSalaryStructure created = salaryStructureService.createSalaryStructure(request);

        assertThat(created).isNotNull();
        assertThat(created.getCompanyId()).isEqualTo(tenantId);
        assertThat(created.getEmployeeId()).isEqualTo(employeeId);
        assertThat(created.getMonthlyGross()).isEqualByComparingTo(new BigDecimal("100000.00"));
        assertThat(created.getEffectiveFrom()).isEqualTo(newEffectiveDate);
        assertThat(created.getEffectiveTo()).isNull();

        assertThat(previousActive.getEffectiveTo()).isEqualTo(newEffectiveDate.minusDays(1));
        verify(basicWageValidator).validateOrThrow(eq(request));
    }

    @Test
    @DisplayName("Create salary structure aborts when BasicWageValidator throws IllegalArgumentException")
    void shouldAbortWhenWageValidatorThrows() {
        Employee employee = Employee.builder().build();
        employee.setId(employeeId);
        employee.setCompanyId(tenantId);

        when(employeeRepository.findByIdAndCompanyId(eq(employeeId), eq(tenantId)))
                .thenReturn(Optional.of(employee));

        SalaryOfferRequest request = SalaryOfferRequest.builder()
                .employeeId(employeeId)
                .monthlyGross(new BigDecimal("100000.00"))
                .basicPay(new BigDecimal("30000.00"))
                .effectiveFrom(LocalDate.of(2026, 4, 1))
                .build();

        doThrow(new IllegalArgumentException("Code on Wages 2019 violation"))
                .when(basicWageValidator).validateOrThrow(eq(request));

        assertThatThrownBy(() -> salaryStructureService.createSalaryStructure(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Code on Wages 2019 violation");

        verify(salaryStructureRepository, never()).save(any());
    }

    @Test
    @DisplayName("getActiveSalaryStructure returns active structure for employee")
    void shouldReturnActiveSalaryStructure() {
        Employee employee = Employee.builder().build();
        employee.setId(employeeId);
        employee.setCompanyId(tenantId);

        when(employeeRepository.findByIdAndCompanyId(eq(employeeId), eq(tenantId)))
                .thenReturn(Optional.of(employee));

        EmployeeSalaryStructure structure = EmployeeSalaryStructure.builder()
                .employeeId(employeeId)
                .monthlyGross(new BigDecimal("100000.00"))
                .basicPay(new BigDecimal("50000.00"))
                .build();

        when(salaryStructureRepository.findActiveStructure(eq(tenantId), eq(employeeId), any(LocalDate.class)))
                .thenReturn(Optional.of(structure));

        EmployeeSalaryStructure active = salaryStructureService.getActiveSalaryStructure(employeeId);

        assertThat(active).isNotNull();
        assertThat(active.getMonthlyGross()).isEqualByComparingTo(new BigDecimal("100000.00"));
    }

    @Test
    @DisplayName("getActiveSalaryStructure throws ResourceNotFoundException when none is active")
    void shouldThrowWhenNoActiveStructure() {
        Employee employee = Employee.builder().build();
        employee.setId(employeeId);
        employee.setCompanyId(tenantId);

        when(employeeRepository.findByIdAndCompanyId(eq(employeeId), eq(tenantId)))
                .thenReturn(Optional.of(employee));

        when(salaryStructureRepository.findActiveStructure(eq(tenantId), eq(employeeId), any(LocalDate.class)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> salaryStructureService.getActiveSalaryStructure(employeeId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Active salary structure not found for employee: " + employeeId);
    }
}

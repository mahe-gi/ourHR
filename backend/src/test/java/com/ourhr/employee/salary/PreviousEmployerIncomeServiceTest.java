package com.ourhr.employee.salary;

import com.ourhr.common.ResourceNotFoundException;
import com.ourhr.common.TenantContext;
import com.ourhr.employee.Employee;
import com.ourhr.employee.EmployeeRepository;
import com.ourhr.employee.salary.dto.PreviousEmployerIncomeResponse;
import com.ourhr.employee.salary.dto.SubmitForm12BRequest;
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
class PreviousEmployerIncomeServiceTest {

    @Mock
    private PreviousEmployerIncomeRepository previousEmployerIncomeRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private PreviousEmployerIncomeService previousEmployerIncomeService;

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
    @DisplayName("Persist Form 12B with high-precision BigDecimal values")
    void shouldPersistForm12BWithBigDecimalPrecision() {
        Employee employee = Employee.builder().build();
        employee.setId(employeeId);
        employee.setCompanyId(tenantId);

        when(employeeRepository.findByIdAndCompanyId(eq(employeeId), eq(tenantId)))
                .thenReturn(Optional.of(employee));
        when(previousEmployerIncomeRepository.findByCompanyIdAndEmployeeId(eq(tenantId), eq(employeeId)))
                .thenReturn(Optional.empty());

        when(previousEmployerIncomeRepository.save(any(PreviousEmployerIncome.class)))
                .thenAnswer(invocation -> {
                    PreviousEmployerIncome income = invocation.getArgument(0);
                    income.setId(UUID.randomUUID());
                    return income;
                });

        SubmitForm12BRequest request = SubmitForm12BRequest.builder()
                .employeeId(employeeId)
                .financialYear("2025-2026")
                .grossSalary(new BigDecimal("450000.50"))
                .pfDeducted(new BigDecimal("21600.00"))
                .ptDeducted(new BigDecimal("2500.00"))
                .tdsDeducted(new BigDecimal("15450.75"))
                .form12bDocumentUrl("https://s3.local/docs/form12b.pdf")
                .build();

        PreviousEmployerIncomeResponse response = previousEmployerIncomeService.savePreviousIncome(request);

        ArgumentCaptor<PreviousEmployerIncome> captor = ArgumentCaptor.forClass(PreviousEmployerIncome.class);
        verify(previousEmployerIncomeRepository).save(captor.capture());
        PreviousEmployerIncome saved = captor.getValue();

        assertThat(saved.getCompanyId()).isEqualTo(tenantId);
        assertThat(saved.getEmployeeId()).isEqualTo(employeeId);
        assertThat(saved.getFinancialYear()).isEqualTo("2025-2026");
        assertThat(saved.getGrossSalary()).isEqualByComparingTo(new BigDecimal("450000.50"));
        assertThat(saved.getPfDeducted()).isEqualByComparingTo(new BigDecimal("21600.00"));
        assertThat(saved.getPtDeducted()).isEqualByComparingTo(new BigDecimal("2500.00"));
        assertThat(saved.getTdsDeducted()).isEqualByComparingTo(new BigDecimal("15450.75"));

        assertThat(response.getGrossSalary()).isEqualByComparingTo(new BigDecimal("450000.50"));
        assertThat(response.getPfDeducted()).isEqualByComparingTo(new BigDecimal("21600.00"));
        assertThat(response.getPtDeducted()).isEqualByComparingTo(new BigDecimal("2500.00"));
        assertThat(response.getTdsDeducted()).isEqualByComparingTo(new BigDecimal("15450.75"));
        assertThat(response.isVerified()).isFalse();
    }

    @Test
    @DisplayName("Defaults null pf, pt, and tds deductions to BigDecimal.ZERO")
    void shouldDefaultNullDeductionsToZero() {
        Employee employee = Employee.builder().build();
        employee.setId(employeeId);
        employee.setCompanyId(tenantId);

        when(employeeRepository.findByIdAndCompanyId(eq(employeeId), eq(tenantId)))
                .thenReturn(Optional.of(employee));
        when(previousEmployerIncomeRepository.findByCompanyIdAndEmployeeId(eq(tenantId), eq(employeeId)))
                .thenReturn(Optional.empty());
        when(previousEmployerIncomeRepository.save(any(PreviousEmployerIncome.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SubmitForm12BRequest request = SubmitForm12BRequest.builder()
                .employeeId(employeeId)
                .financialYear("2025-2026")
                .grossSalary(new BigDecimal("300000.00"))
                .pfDeducted(null)
                .ptDeducted(null)
                .tdsDeducted(null)
                .build();

        PreviousEmployerIncomeResponse response = previousEmployerIncomeService.savePreviousIncome(request);

        assertThat(response.getPfDeducted()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getPtDeducted()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getTdsDeducted()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Throws ResourceNotFoundException when employee does not belong to tenant")
    void shouldThrowWhenEmployeeNotFoundInTenant() {
        when(employeeRepository.findByIdAndCompanyId(eq(employeeId), eq(tenantId)))
                .thenReturn(Optional.empty());

        SubmitForm12BRequest request = SubmitForm12BRequest.builder()
                .employeeId(employeeId)
                .financialYear("2025-2026")
                .grossSalary(new BigDecimal("100000.00"))
                .build();

        assertThatThrownBy(() -> previousEmployerIncomeService.savePreviousIncome(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Employee not found with id: " + employeeId);

        verify(previousEmployerIncomeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Throws IllegalArgumentException when TenantContext is missing")
    void shouldThrowWhenTenantContextMissing() {
        TenantContext.clear();

        SubmitForm12BRequest request = SubmitForm12BRequest.builder()
                .employeeId(employeeId)
                .build();

        assertThatThrownBy(() -> previousEmployerIncomeService.savePreviousIncome(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tenant context is required");

        assertThatThrownBy(() -> previousEmployerIncomeService.getPreviousIncome(employeeId, "2025-2026"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tenant context is required");
    }

    @Test
    @DisplayName("getPreviousIncome retrieves Form 12B record for employee and FY")
    void shouldRetrievePreviousIncomeSuccessfully() {
        Employee employee = Employee.builder().build();
        employee.setId(employeeId);
        employee.setCompanyId(tenantId);

        PreviousEmployerIncome income = PreviousEmployerIncome.builder()
                .employeeId(employeeId)
                .financialYear("2025-2026")
                .grossSalary(new BigDecimal("600000.00"))
                .pfDeducted(new BigDecimal("21600.00"))
                .ptDeducted(new BigDecimal("2500.00"))
                .tdsDeducted(new BigDecimal("25000.00"))
                .isVerified(true)
                .build();
        income.setId(UUID.randomUUID());
        income.setCompanyId(tenantId);

        when(employeeRepository.findByIdAndCompanyId(eq(employeeId), eq(tenantId)))
                .thenReturn(Optional.of(employee));
        when(previousEmployerIncomeRepository.findByCompanyIdAndEmployeeIdAndFinancialYear(eq(tenantId), eq(employeeId), eq("2025-2026")))
                .thenReturn(Optional.of(income));

        PreviousEmployerIncomeResponse response = previousEmployerIncomeService.getPreviousIncome(employeeId, "2025-2026");

        assertThat(response).isNotNull();
        assertThat(response.getGrossSalary()).isEqualByComparingTo(new BigDecimal("600000.00"));
        assertThat(response.getFinancialYear()).isEqualTo("2025-2026");
        assertThat(response.isVerified()).isTrue();
    }

    @Test
    @DisplayName("getPreviousIncome throws ResourceNotFoundException when Form 12B record does not exist")
    void shouldThrowWhenForm12BNotFound() {
        Employee employee = Employee.builder().build();
        employee.setId(employeeId);
        employee.setCompanyId(tenantId);

        when(employeeRepository.findByIdAndCompanyId(eq(employeeId), eq(tenantId)))
                .thenReturn(Optional.of(employee));
        when(previousEmployerIncomeRepository.findByCompanyIdAndEmployeeIdAndFinancialYear(eq(tenantId), eq(employeeId), eq("2025-2026")))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> previousEmployerIncomeService.getPreviousIncome(employeeId, "2025-2026"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Form 12B record not found for employee: " + employeeId);
    }
}

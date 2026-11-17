package com.ourhr.payroll;

import com.ourhr.auth.User;
import com.ourhr.auth.UserRepository;
import com.ourhr.common.ResourceNotFoundException;
import com.ourhr.common.TenantContext;
import com.ourhr.employee.Employee;
import com.ourhr.employee.EmployeeRepository;
import com.ourhr.employee.salary.EmployeeSalaryStructure;
import com.ourhr.employee.salary.SalaryStructureService;
import com.ourhr.payroll.dto.CreateLopReversalRequest;
import com.ourhr.payroll.dto.LopReversalResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LopReversalServiceTest {

    @Mock
    private LopReversalRepository lopReversalRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private SalaryStructureService salaryStructureService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private LopReversalService lopReversalService;

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
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should create LOP reversal and compute reversal amount correctly")
    void shouldCreateLopReversal() {
        Employee employee = Employee.builder()
                .firstName("Mahesh")
                .lastName("Ch")
                .employeeCode("EMP001")
                .build();
        employee.setId(employeeId);
        employee.setCompanyId(tenantId);

        EmployeeSalaryStructure structure = EmployeeSalaryStructure.builder()
                .employeeId(employeeId)
                .monthlyGross(new BigDecimal("60000.00"))
                .basicPay(new BigDecimal("30000.00"))
                .hraPay(new BigDecimal("15000.00"))
                .specialAllowance(new BigDecimal("15000.00"))
                .build();

        when(employeeRepository.findByIdAndCompanyId(employeeId, tenantId)).thenReturn(Optional.of(employee));
        when(salaryStructureService.findActiveStructure(eq(employeeId), any(LocalDate.class))).thenReturn(Optional.of(structure));

        when(lopReversalRepository.save(any(LopReversal.class))).thenAnswer(invocation -> {
            LopReversal rev = invocation.getArgument(0);
            rev.setId(UUID.randomUUID());
            return rev;
        });

        CreateLopReversalRequest request = CreateLopReversalRequest.builder()
                .employeeId(employeeId)
                .originalPayrollMonth(4)
                .originalPayrollYear(2026)
                .reversedDays(new BigDecimal("2.0"))
                .reason("Missed swipe regularized")
                .build();

        LopReversalResponse response = lopReversalService.createLopReversal(request);

        assertThat(response).isNotNull();
        assertThat(response.getEmployeeId()).isEqualTo(employeeId);
        assertThat(response.getStatus()).isEqualTo("PENDING");
        // Month 4 has 30 days. Daily rate = 60,000 / 30 = 2,000. Reversed days = 2.0 -> reversal amount = 4,000.00
        assertThat(response.getReversalAmount()).isEqualByComparingTo(new BigDecimal("4000.00"));
    }

    @Test
    @DisplayName("Should approve pending LOP reversal")
    void shouldApproveLopReversal() {
        UUID reversalId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        LopReversal pendingReversal = LopReversal.builder()
                .employeeId(employeeId)
                .originalPayrollMonth(4)
                .originalPayrollYear(2026)
                .reversedDays(new BigDecimal("1.0"))
                .reversalAmount(new BigDecimal("2000.00"))
                .status("PENDING")
                .build();
        pendingReversal.setId(reversalId);
        pendingReversal.setCompanyId(tenantId);

        Employee employee = Employee.builder()
                .firstName("Mahesh")
                .lastName("Ch")
                .build();
        employee.setId(employeeId);

        User user = User.builder().email("admin@ourhr.com").build();
        user.setId(userId);

        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn("admin@ourhr.com");
        SecurityContext secCtx = mock(SecurityContext.class);
        when(secCtx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(secCtx);

        when(userRepository.findByEmail("admin@ourhr.com")).thenReturn(Optional.of(user));
        when(lopReversalRepository.findByCompanyIdAndId(tenantId, reversalId)).thenReturn(Optional.of(pendingReversal));
        when(lopReversalRepository.save(any(LopReversal.class))).thenAnswer(i -> i.getArgument(0));
        when(employeeRepository.findByIdAndCompanyId(employeeId, tenantId)).thenReturn(Optional.of(employee));

        LopReversalResponse approved = lopReversalService.approveLopReversal(reversalId);

        assertThat(approved.getStatus()).isEqualTo("APPROVED");
        assertThat(approved.getApprovedBy()).isEqualTo(userId);
    }

    @Test
    @DisplayName("Should throw when approving non-pending reversal")
    void shouldThrowWhenApprovingNonPendingReversal() {
        UUID reversalId = UUID.randomUUID();
        LopReversal processed = LopReversal.builder()
                .employeeId(employeeId)
                .status("PROCESSED")
                .build();
        processed.setId(reversalId);
        processed.setCompanyId(tenantId);

        when(lopReversalRepository.findByCompanyIdAndId(tenantId, reversalId)).thenReturn(Optional.of(processed));

        assertThatThrownBy(() -> lopReversalService.approveLopReversal(reversalId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already in status: PROCESSED");
    }

    @Test
    @DisplayName("Should list pending reversals")
    void shouldListPendingReversals() {
        LopReversal rev = LopReversal.builder()
                .employeeId(employeeId)
                .originalPayrollMonth(4)
                .originalPayrollYear(2026)
                .reversedDays(new BigDecimal("1.0"))
                .status("PENDING")
                .reversalAmount(new BigDecimal("1500.00"))
                .build();
        rev.setId(UUID.randomUUID());
        rev.setCompanyId(tenantId);

        when(lopReversalRepository.findByCompanyIdAndStatus(tenantId, "PENDING")).thenReturn(List.of(rev));
        when(employeeRepository.findByIdAndCompanyId(employeeId, tenantId)).thenReturn(Optional.empty());

        List<LopReversalResponse> pending = lopReversalService.getPendingReversals();
        assertThat(pending).hasSize(1);
    }
}

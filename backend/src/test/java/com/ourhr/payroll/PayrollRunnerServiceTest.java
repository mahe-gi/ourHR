package com.ourhr.payroll;

import com.ourhr.attendance.MonthlyAttendanceSummary;
import com.ourhr.attendance.MonthlyAttendanceSummaryRepository;
import com.ourhr.auth.User;
import com.ourhr.auth.UserRepository;
import com.ourhr.common.TenantContext;
import com.ourhr.company.BranchService;
import com.ourhr.company.dto.BranchResponse;
import com.ourhr.employee.Employee;
import com.ourhr.employee.EmployeeRepository;
import com.ourhr.employee.salary.EmployeeSalaryStructure;
import com.ourhr.employee.salary.SalaryStructureService;
import com.ourhr.payroll.dto.InitiatePayrollRunRequest;
import com.ourhr.payroll.dto.PayrollAuditDiffResponse;
import com.ourhr.payroll.dto.PayrollRunResponse;
import com.ourhr.payroll.dto.PayslipSummaryDto;
import com.ourhr.statutory.PtSlab;
import com.ourhr.statutory.StatutoryQueryService;
import com.ourhr.statutory.TdsTaxSlab;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayrollRunnerServiceTest {

    @Mock
    private PayrollRunRepository payrollRunRepository;

    @Mock
    private PayslipLineItemRepository payslipLineItemRepository;

    @Mock
    private LopReversalRepository lopReversalRepository;

    @Mock
    private MonthlyAttendanceSummaryRepository monthlyAttendanceSummaryRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private SalaryStructureService salaryStructureService;

    @Mock
    private StatutoryQueryService statutoryQueryService;

    @Mock
    private BranchService branchService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PayrollAuditService payrollAuditService;

    @InjectMocks
    private PayrollRunnerService payrollRunnerService;

    private UUID tenantId;
    private UUID employeeId;
    private UUID runId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        employeeId = UUID.randomUUID();
        runId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should successfully initiate a new payroll run in DRAFT status")
    void shouldInitiatePayrollRun() {
        InitiatePayrollRunRequest request = InitiatePayrollRunRequest.builder()
                .payrollMonth(4)
                .payrollYear(2026)
                .build();

        when(payrollRunRepository.existsByCompanyIdAndPayrollMonthAndPayrollYear(tenantId, 4, 2026))
                .thenReturn(false);

        User user = User.builder().email("hr@ourhr.com").build();
        user.setId(UUID.randomUUID());
        user.setCompanyId(tenantId);
        when(userRepository.findAll()).thenReturn(List.of(user));

        when(payrollRunRepository.save(any(PayrollRun.class))).thenAnswer(i -> {
            PayrollRun r = i.getArgument(0);
            r.setId(runId);
            return r;
        });

        PayrollRunResponse response = payrollRunnerService.initiatePayrollRun(request);

        assertThat(response).isNotNull();
        assertThat(response.getRunStatus()).isEqualTo("DRAFT");
        assertThat(response.getPayrollMonth()).isEqualTo(4);
        assertThat(response.getPayrollYear()).isEqualTo(2026);
    }

    @Test
    @DisplayName("Should throw when duplicate payroll run is initiated")
    void shouldThrowOnDuplicateInitiation() {
        InitiatePayrollRunRequest request = InitiatePayrollRunRequest.builder()
                .payrollMonth(4)
                .payrollYear(2026)
                .build();

        when(payrollRunRepository.existsByCompanyIdAndPayrollMonthAndPayrollYear(tenantId, 4, 2026))
                .thenReturn(true);

        assertThatThrownBy(() -> payrollRunnerService.initiatePayrollRun(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("Should lock attendance when all monthly summaries are locked")
    void shouldLockAttendance() {
        PayrollRun run = PayrollRun.builder()
                .payrollMonth(4)
                .payrollYear(2026)
                .runStatus("DRAFT")
                .build();
        run.setId(runId);
        run.setCompanyId(tenantId);

        MonthlyAttendanceSummary summary = MonthlyAttendanceSummary.builder()
                .payrollMonth(4)
                .payrollYear(2026)
                .isLocked(true)
                .build();

        when(payrollRunRepository.findByCompanyIdAndId(tenantId, runId)).thenReturn(Optional.of(run));
        when(monthlyAttendanceSummaryRepository.findByCompanyIdAndPayrollMonthAndPayrollYear(tenantId, 4, 2026))
                .thenReturn(List.of(summary));
        when(payrollRunRepository.save(any(PayrollRun.class))).thenAnswer(i -> i.getArgument(0));

        PayrollRunResponse response = payrollRunnerService.lockAttendance(runId);

        assertThat(response.getRunStatus()).isEqualTo("ATTENDANCE_LOCKED");
    }

    @Test
    @DisplayName("Should reject attendance lock if monthly summary is not locked")
    void shouldRejectAttendanceLockIfUnlocked() {
        PayrollRun run = PayrollRun.builder()
                .payrollMonth(4)
                .payrollYear(2026)
                .runStatus("DRAFT")
                .build();
        run.setId(runId);
        run.setCompanyId(tenantId);

        MonthlyAttendanceSummary summary = MonthlyAttendanceSummary.builder()
                .payrollMonth(4)
                .payrollYear(2026)
                .isLocked(false)
                .build();

        when(payrollRunRepository.findByCompanyIdAndId(tenantId, runId)).thenReturn(Optional.of(run));
        when(monthlyAttendanceSummaryRepository.findByCompanyIdAndPayrollMonthAndPayrollYear(tenantId, 4, 2026))
                .thenReturn(List.of(summary));

        assertThatThrownBy(() -> payrollRunnerService.lockAttendance(runId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("is not locked");
    }

    @Test
    @DisplayName("Should apply approved LOP adjustments")
    void shouldApplyAdjustments() {
        PayrollRun run = PayrollRun.builder()
                .payrollMonth(4)
                .payrollYear(2026)
                .runStatus("ATTENDANCE_LOCKED")
                .build();
        run.setId(runId);
        run.setCompanyId(tenantId);

        LopReversal rev = LopReversal.builder()
                .employeeId(employeeId)
                .status("APPROVED")
                .reversalAmount(new BigDecimal("3000.00"))
                .build();

        when(payrollRunRepository.findByCompanyIdAndId(tenantId, runId)).thenReturn(Optional.of(run));
        when(lopReversalRepository.findByCompanyIdAndStatus(tenantId, "APPROVED")).thenReturn(List.of(rev));
        when(payrollRunRepository.save(any(PayrollRun.class))).thenAnswer(i -> i.getArgument(0));

        PayrollRunResponse response = payrollRunnerService.applyAdjustments(runId);

        assertThat(response.getRunStatus()).isEqualTo("ADJUSTMENTS_APPLIED");
        assertThat(rev.getStatus()).isEqualTo("PROCESSED");
        assertThat(rev.getProcessedInPayrollRunId()).isEqualTo(runId);
    }

    @Test
    @DisplayName("Should calculate payroll with proration, EPF ceiling, ESIC, and PT")
    void shouldCalculatePayroll() {
        PayrollRun run = PayrollRun.builder()
                .payrollMonth(4)
                .payrollYear(2026)
                .runStatus("ADJUSTMENTS_APPLIED")
                .build();
        run.setId(runId);
        run.setCompanyId(tenantId);

        UUID branchId = UUID.randomUUID();
        Employee employee = Employee.builder()
                .firstName("Ramesh")
                .lastName("Kumar")
                .employeeCode("EMP001")
                .employmentStatus("ACTIVE")
                .workerType("REGULAR")
                .gender("MALE")
                .branchId(branchId)
                .build();
        employee.setId(employeeId);
        employee.setCompanyId(tenantId);

        EmployeeSalaryStructure structure = EmployeeSalaryStructure.builder()
                .employeeId(employeeId)
                .monthlyGross(new BigDecimal("50000.00"))
                .basicPay(new BigDecimal("25000.00"))
                .daPay(BigDecimal.ZERO)
                .hraPay(new BigDecimal("12500.00"))
                .specialAllowance(new BigDecimal("12500.00"))
                .pfOptOut(false)
                .build();

        MonthlyAttendanceSummary attendance = MonthlyAttendanceSummary.builder()
                .employeeId(employeeId)
                .payrollMonth(4)
                .payrollYear(2026)
                .totalCalendarDays(30)
                .payableDays(new BigDecimal("30.0"))
                .isLocked(true)
                .build();

        when(payrollRunRepository.findByCompanyIdAndId(tenantId, runId)).thenReturn(Optional.of(run));
        when(employeeRepository.findByCompanyId(tenantId)).thenReturn(List.of(employee));
        when(monthlyAttendanceSummaryRepository.findByCompanyIdAndPayrollMonthAndPayrollYear(tenantId, 4, 2026))
                .thenReturn(List.of(attendance));
        when(lopReversalRepository.findByCompanyIdAndProcessedInPayrollRunId(tenantId, runId)).thenReturn(List.of());

        when(statutoryQueryService.getEpfWageCeiling(any(LocalDate.class))).thenReturn(new BigDecimal("15000.00"));
        when(statutoryQueryService.getEsicWageCeiling(any(LocalDate.class))).thenReturn(new BigDecimal("21000.00"));
        when(salaryStructureService.findActiveStructure(eq(employeeId), any(LocalDate.class))).thenReturn(Optional.of(structure));

        BranchResponse branch = BranchResponse.builder().stateCode("KA").build();
        when(branchService.getBranchById(branchId)).thenReturn(branch);

        PtSlab slab = PtSlab.builder().taxAmount(new BigDecimal("200.00")).build();
        when(statutoryQueryService.getPtSlab(eq("KA"), eq("MALE"), any(BigDecimal.class), any(LocalDate.class)))
                .thenReturn(Optional.of(slab));
        when(statutoryQueryService.getTdsSlabs(any(), eq("NEW"))).thenReturn(List.of());

        when(payrollRunRepository.save(any(PayrollRun.class))).thenAnswer(i -> i.getArgument(0));

        PayrollRunResponse response = payrollRunnerService.calculatePayroll(runId);

        assertThat(response.getRunStatus()).isEqualTo("CALCULATED");
        // Gross: 50,000.00
        assertThat(response.getTotalGrossPay()).isEqualByComparingTo(new BigDecimal("50000.00"));
        // EPF capped at 15,000 -> 12% = 1,800.00 employee + 1,800.00 employer = 3,600.00 PF liability
        assertThat(response.getTotalPfLiability()).isEqualByComparingTo(new BigDecimal("3600.00"));
        // Gross 50,000 > ESIC ceiling 21,000 -> ESIC = 0.00
        assertThat(response.getTotalEsicLiability()).isEqualByComparingTo(BigDecimal.ZERO);
        // PT = 200.00
        assertThat(response.getTotalPtLiability()).isEqualByComparingTo(new BigDecimal("200.00"));
        // Net pay = 50,000 - 1,800 (EPF) - 200 (PT) = 48,000.00
        assertThat(response.getTotalNetPay()).isEqualByComparingTo(new BigDecimal("48000.00"));

        verify(payslipLineItemRepository).deleteByPayrollRunIdAndCompanyId(runId, tenantId);
    }

    @Test
    @DisplayName("Should approve payroll when user has ROLE_FOUNDER")
    void shouldApprovePayroll() {
        PayrollRun run = PayrollRun.builder()
                .payrollMonth(4)
                .payrollYear(2026)
                .runStatus("AUDITED")
                .build();
        run.setId(runId);
        run.setCompanyId(tenantId);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("founder@ourhr.com", "pass",
                        List.of(new SimpleGrantedAuthority("ROLE_FOUNDER"))));

        when(payrollRunRepository.findByCompanyIdAndId(tenantId, runId)).thenReturn(Optional.of(run));
        when(payrollRunRepository.save(any(PayrollRun.class))).thenAnswer(i -> i.getArgument(0));

        PayrollRunResponse response = payrollRunnerService.approvePayroll(runId);

        assertThat(response.getRunStatus()).isEqualTo("APPROVED");
        assertThat(response.getApprovedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should reject approval when user lacks FOUNDER or FINANCE role")
    void shouldRejectUnauthorizedApproval() {
        PayrollRun run = PayrollRun.builder()
                .runStatus("AUDITED")
                .build();
        run.setId(runId);
        run.setCompanyId(tenantId);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("emp@ourhr.com", "pass",
                        List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))));

        when(payrollRunRepository.findByCompanyIdAndId(tenantId, runId)).thenReturn(Optional.of(run));

        assertThatThrownBy(() -> payrollRunnerService.approvePayroll(runId))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("Should disburse approved payroll and prohibit subsequent recalculations")
    void shouldDisburseAndLockImmutably() {
        PayrollRun run = PayrollRun.builder()
                .payrollMonth(4)
                .payrollYear(2026)
                .runStatus("APPROVED")
                .build();
        run.setId(runId);
        run.setCompanyId(tenantId);

        when(payrollRunRepository.findByCompanyIdAndId(tenantId, runId)).thenReturn(Optional.of(run));
        when(payrollRunRepository.save(any(PayrollRun.class))).thenAnswer(i -> i.getArgument(0));

        PayrollRunResponse disbursed = payrollRunnerService.disbursePayroll(runId);
        assertThat(disbursed.getRunStatus()).isEqualTo("DISBURSED");

        // Attempting to calculate disbursed run must throw IllegalStateException
        assertThatThrownBy(() -> payrollRunnerService.calculatePayroll(runId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("immutable");
    }

    @Test
    @DisplayName("Should retrieve payslip summary for an employee")
    void shouldGetPayslipSummary() {
        PayrollRun run = PayrollRun.builder()
                .payrollMonth(4)
                .payrollYear(2026)
                .runStatus("DISBURSED")
                .build();
        run.setId(runId);
        run.setCompanyId(tenantId);

        Employee emp = Employee.builder()
                .employeeCode("EMP001")
                .firstName("Mahesh")
                .lastName("Ch")
                .department("Engineering")
                .designation("Staff Engineer")
                .build();
        emp.setId(employeeId);
        emp.setCompanyId(tenantId);

        PayslipLineItem earning = PayslipLineItem.builder()
                .payrollRunId(runId)
                .employeeId(employeeId)
                .itemCategory("EARNING")
                .itemName("Basic Pay")
                .itemAmount(new BigDecimal("50000.00"))
                .build();

        PayslipLineItem deduction = PayslipLineItem.builder()
                .payrollRunId(runId)
                .employeeId(employeeId)
                .itemCategory("STATUTORY_DEDUCTION")
                .itemName("Employee PF Contribution")
                .itemAmount(new BigDecimal("1800.00"))
                .build();

        when(payrollRunRepository.findByCompanyIdAndId(tenantId, runId)).thenReturn(Optional.of(run));
        when(employeeRepository.findByIdAndCompanyId(employeeId, tenantId)).thenReturn(Optional.of(emp));
        when(payslipLineItemRepository.findByCompanyIdAndPayrollRunIdAndEmployeeId(tenantId, runId, employeeId))
                .thenReturn(List.of(earning, deduction));

        PayslipSummaryDto summary = payrollRunnerService.getPayslipSummary(runId, employeeId);

        assertThat(summary).isNotNull();
        assertThat(summary.getEmployeeName()).isEqualTo("Mahesh Ch");
        assertThat(summary.getGrossEarnings()).isEqualByComparingTo(new BigDecimal("50000.00"));
        assertThat(summary.getTotalDeductions()).isEqualByComparingTo(new BigDecimal("1800.00"));
        assertThat(summary.getNetPay()).isEqualByComparingTo(new BigDecimal("48200.00"));
        assertThat(summary.getLineItems()).hasSize(2);
    }
}

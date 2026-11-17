package com.ourhr.payroll;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.ourhr.payroll.dto.ConsultantPayoutRequest;
import com.ourhr.payroll.dto.ConsultantPayoutResponse;
import com.ourhr.payroll.dto.InitiatePayrollRunRequest;
import com.ourhr.payroll.dto.LopReversalResponse;
import com.ourhr.payroll.dto.PayrollAuditDiffResponse;
import com.ourhr.payroll.dto.PayrollRunResponse;
import com.ourhr.payroll.dto.PayrollStepTransitionRequest;
import com.ourhr.payroll.dto.PayslipSummaryDto;
import com.ourhr.security.JwtAuthenticationFilter;
import com.ourhr.security.JwtService;
import com.ourhr.security.SecurityConfig;
import com.ourhr.statutory.PtSlab;
import com.ourhr.statutory.StatutoryQueryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        PayrollController.class,
        LopReversalController.class,
        ConsultantPayoutController.class
})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class ImmutablePayrollDisbursalTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PayrollRunnerService mockPayrollRunnerService;

    @MockBean
    private LopReversalService mockLopReversalService;

    @MockBean
    private ConsultantPayoutService mockConsultantPayoutService;

    @MockBean
    private JwtService jwtService;

    // Direct unit test dependencies for PayrollRunnerService
    private PayrollRunRepository payrollRunRepository;
    private PayslipLineItemRepository payslipLineItemRepository;
    private LopReversalRepository lopReversalRepository;
    private MonthlyAttendanceSummaryRepository monthlyAttendanceSummaryRepository;
    private EmployeeRepository employeeRepository;
    private SalaryStructureService salaryStructureService;
    private StatutoryQueryService statutoryQueryService;
    private BranchService branchService;
    private UserRepository userRepository;
    private PayrollAuditService payrollAuditService;
    private PayrollRunnerService unitRunnerService;

    private UUID tenantId;
    private UUID runId;
    private UUID employeeId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        runId = UUID.randomUUID();
        employeeId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);

        payrollRunRepository = mock(PayrollRunRepository.class);
        payslipLineItemRepository = mock(PayslipLineItemRepository.class);
        lopReversalRepository = mock(LopReversalRepository.class);
        monthlyAttendanceSummaryRepository = mock(MonthlyAttendanceSummaryRepository.class);
        employeeRepository = mock(EmployeeRepository.class);
        salaryStructureService = mock(SalaryStructureService.class);
        statutoryQueryService = mock(StatutoryQueryService.class);
        branchService = mock(BranchService.class);
        userRepository = mock(UserRepository.class);
        payrollAuditService = mock(PayrollAuditService.class);

        unitRunnerService = new PayrollRunnerService(
                payrollRunRepository,
                payslipLineItemRepository,
                lopReversalRepository,
                monthlyAttendanceSummaryRepository,
                employeeRepository,
                salaryStructureService,
                statutoryQueryService,
                branchService,
                userRepository,
                payrollAuditService
        );
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    // =========================================================================
    // SECTION 1: IMMUTABLE PAYROLL LAW VERIFICATION (UNIT TESTS)
    // =========================================================================

    @Test
    @DisplayName("Immutable Law: Disbursed payroll run cannot be recalculated and throws IllegalStateException")
    void disbursedRunCannotBeRecalculated() {
        PayrollRun run = PayrollRun.builder()
                .payrollMonth(4)
                .payrollYear(2026)
                .runStatus("DISBURSED")
                .build();
        run.setId(runId);
        run.setCompanyId(tenantId);

        when(payrollRunRepository.findByCompanyIdAndId(tenantId, runId)).thenReturn(Optional.of(run));

        assertThatThrownBy(() -> unitRunnerService.calculatePayroll(runId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("immutable");

        // Verify that line item purge or save was NEVER invoked
        verify(payslipLineItemRepository, never()).deleteByPayrollRunIdAndCompanyId(any(), any());
        verify(payslipLineItemRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Immutable Law: Approved payroll run cannot be recalculated and throws IllegalStateException")
    void approvedRunCannotBeRecalculated() {
        PayrollRun run = PayrollRun.builder()
                .payrollMonth(4)
                .payrollYear(2026)
                .runStatus("APPROVED")
                .build();
        run.setId(runId);
        run.setCompanyId(tenantId);

        when(payrollRunRepository.findByCompanyIdAndId(tenantId, runId)).thenReturn(Optional.of(run));

        assertThatThrownBy(() -> unitRunnerService.calculatePayroll(runId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot recalculate payroll run in status APPROVED");

        verify(payslipLineItemRepository, never()).deleteByPayrollRunIdAndCompanyId(any(), any());
        verify(payslipLineItemRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Immutable Law: Payslip line items for DISBURSED run cannot be mutated")
    void payslipLineItemsForDisbursedRunCannotBeMutated() {
        PayrollRun run = PayrollRun.builder()
                .payrollMonth(4)
                .payrollYear(2026)
                .runStatus("DISBURSED")
                .build();
        run.setId(runId);
        run.setCompanyId(tenantId);

        when(payrollRunRepository.findByCompanyIdAndId(tenantId, runId)).thenReturn(Optional.of(run));

        assertThatThrownBy(() -> unitRunnerService.calculatePayroll(runId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("immutable");

        assertThatThrownBy(() -> unitRunnerService.applyAdjustments(runId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("immutable");

        verify(payslipLineItemRepository, never()).deleteByPayrollRunIdAndCompanyId(any(), any());
        verify(payslipLineItemRepository, never()).save(any());
        verify(payslipLineItemRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Immutable Law: Disbursed payroll run rejects lockAttendance, audit, approve, and disburse")
    void disbursedRunRejectsAllStateMutations() {
        PayrollRun run = PayrollRun.builder()
                .payrollMonth(4)
                .payrollYear(2026)
                .runStatus("DISBURSED")
                .build();
        run.setId(runId);
        run.setCompanyId(tenantId);

        when(payrollRunRepository.findByCompanyIdAndId(tenantId, runId)).thenReturn(Optional.of(run));

        assertThatThrownBy(() -> unitRunnerService.lockAttendance(runId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("immutable");

        assertThatThrownBy(() -> unitRunnerService.applyAdjustments(runId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("immutable");

        assertThatThrownBy(() -> unitRunnerService.auditPayroll(runId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("immutable");

        assertThatThrownBy(() -> unitRunnerService.approvePayroll(runId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("immutable");

        assertThatThrownBy(() -> unitRunnerService.disbursePayroll(runId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Expected status: APPROVED");
    }

    // =========================================================================
    // SECTION 2: DRAFT RECALCULATION LAW (UNIT TESTS)
    // =========================================================================

    @Test
    @DisplayName("Draft Recalculation Law: Run in CALCULATED status can be recalculated, line items purged and cleanly replaced")
    void calculatedRunRecalculationPurgesAndCleanlyReplacesLineItems() {
        PayrollRun run = PayrollRun.builder()
                .payrollMonth(4)
                .payrollYear(2026)
                .runStatus("CALCULATED")
                .build();
        run.setId(runId);
        run.setCompanyId(tenantId);

        Employee employee = Employee.builder()
                .firstName("Suresh")
                .lastName("Patil")
                .employeeCode("EMP002")
                .employmentStatus("ACTIVE")
                .workerType("REGULAR")
                .gender("MALE")
                .build();
        employee.setId(employeeId);
        employee.setCompanyId(tenantId);

        EmployeeSalaryStructure structure = EmployeeSalaryStructure.builder()
                .employeeId(employeeId)
                .monthlyGross(new BigDecimal("60000.00"))
                .basicPay(new BigDecimal("30000.00"))
                .daPay(BigDecimal.ZERO)
                .hraPay(new BigDecimal("15000.00"))
                .specialAllowance(new BigDecimal("15000.00"))
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
        when(statutoryQueryService.getPtSlab(any(), any(), any(), any())).thenReturn(Optional.empty());
        when(statutoryQueryService.getTdsSlabs(any(), eq("NEW"))).thenReturn(List.of());
        when(payrollRunRepository.save(any(PayrollRun.class))).thenAnswer(i -> i.getArgument(0));

        PayrollRunResponse response = unitRunnerService.calculatePayroll(runId);

        assertThat(response).isNotNull();
        assertThat(response.getRunStatus()).isEqualTo("CALCULATED");

        // Verify draft line items were purged before re-saving
        verify(payslipLineItemRepository, times(1)).deleteByPayrollRunIdAndCompanyId(runId, tenantId);
        verify(payslipLineItemRepository, times(1)).flush();
        verify(payslipLineItemRepository, times(1)).saveAll(any());
    }

    // =========================================================================
    // SECTION 3: STATE MACHINE TRANSITION INTEGRITY (UNIT TESTS)
    // =========================================================================

    @Test
    @DisplayName("State Machine: Valid sequential flow DRAFT -> ATTENDANCE_LOCKED -> ADJUSTMENTS_APPLIED -> CALCULATED -> AUDITED -> APPROVED -> DISBURSED")
    void validSequentialTransitionFlowSucceeds() {
        // Step 0: DRAFT
        PayrollRun run = PayrollRun.builder()
                .payrollMonth(4)
                .payrollYear(2026)
                .runStatus("DRAFT")
                .build();
        run.setId(runId);
        run.setCompanyId(tenantId);

        when(payrollRunRepository.findByCompanyIdAndId(tenantId, runId)).thenReturn(Optional.of(run));
        when(payrollRunRepository.save(any(PayrollRun.class))).thenAnswer(i -> i.getArgument(0));

        // Step 1: lockAttendance
        MonthlyAttendanceSummary summary = MonthlyAttendanceSummary.builder()
                .payrollMonth(4)
                .payrollYear(2026)
                .isLocked(true)
                .build();
        when(monthlyAttendanceSummaryRepository.findByCompanyIdAndPayrollMonthAndPayrollYear(tenantId, 4, 2026))
                .thenReturn(List.of(summary));

        PayrollRunResponse step1 = unitRunnerService.lockAttendance(runId);
        assertThat(step1.getRunStatus()).isEqualTo("ATTENDANCE_LOCKED");
        run.setRunStatus("ATTENDANCE_LOCKED");

        // Step 2: applyAdjustments
        when(lopReversalRepository.findByCompanyIdAndStatus(tenantId, "APPROVED")).thenReturn(List.of());
        PayrollRunResponse step2 = unitRunnerService.applyAdjustments(runId);
        assertThat(step2.getRunStatus()).isEqualTo("ADJUSTMENTS_APPLIED");
        run.setRunStatus("ADJUSTMENTS_APPLIED");

        // Step 3: calculatePayroll
        when(employeeRepository.findByCompanyId(tenantId)).thenReturn(List.of());
        when(statutoryQueryService.getEpfWageCeiling(any(LocalDate.class))).thenReturn(new BigDecimal("15000.00"));
        when(statutoryQueryService.getEsicWageCeiling(any(LocalDate.class))).thenReturn(new BigDecimal("21000.00"));
        PayrollRunResponse step3 = unitRunnerService.calculatePayroll(runId);
        assertThat(step3.getRunStatus()).isEqualTo("CALCULATED");
        run.setRunStatus("CALCULATED");

        // Step 4: auditPayroll
        when(payrollAuditService.auditRun(run)).thenReturn(List.of());
        List<PayrollAuditDiffResponse> diffs = unitRunnerService.auditPayroll(runId);
        assertThat(diffs).isEmpty();
        run.setRunStatus("AUDITED");

        // Step 5: approvePayroll
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("founder@ourhr.com", "pass",
                        List.of(new SimpleGrantedAuthority("ROLE_FOUNDER"))));
        PayrollRunResponse step5 = unitRunnerService.approvePayroll(runId);
        assertThat(step5.getRunStatus()).isEqualTo("APPROVED");
        assertThat(step5.getApprovedAt()).isNotNull();
        run.setRunStatus("APPROVED");

        // Step 6: disbursePayroll
        PayrollRunResponse step6 = unitRunnerService.disbursePayroll(runId);
        assertThat(step6.getRunStatus()).isEqualTo("DISBURSED");
    }

    @Test
    @DisplayName("State Machine: Invalid jump DRAFT -> DISBURSED rejected")
    void invalidJumpDraftToDisbursedRejected() {
        PayrollRun run = PayrollRun.builder()
                .payrollMonth(4)
                .payrollYear(2026)
                .runStatus("DRAFT")
                .build();
        run.setId(runId);
        run.setCompanyId(tenantId);

        when(payrollRunRepository.findByCompanyIdAndId(tenantId, runId)).thenReturn(Optional.of(run));

        PayrollStepTransitionRequest request = PayrollStepTransitionRequest.builder()
                .targetStatus("DISBURSED")
                .build();

        assertThatThrownBy(() -> unitRunnerService.transitionStep(runId, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot disburse payroll run in status DRAFT. Expected status: APPROVED");
    }

    @Test
    @DisplayName("State Machine: Invalid jump DRAFT -> APPROVED rejected")
    void invalidJumpDraftToApprovedRejected() {
        PayrollRun run = PayrollRun.builder()
                .payrollMonth(4)
                .payrollYear(2026)
                .runStatus("DRAFT")
                .build();
        run.setId(runId);
        run.setCompanyId(tenantId);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("founder@ourhr.com", "pass",
                        List.of(new SimpleGrantedAuthority("ROLE_FOUNDER"))));

        when(payrollRunRepository.findByCompanyIdAndId(tenantId, runId)).thenReturn(Optional.of(run));

        PayrollStepTransitionRequest request = PayrollStepTransitionRequest.builder()
                .targetStatus("APPROVED")
                .build();

        assertThatThrownBy(() -> unitRunnerService.transitionStep(runId, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot approve payroll run in status DRAFT. Expected status: AUDITED");
    }

    @Test
    @DisplayName("State Machine: Invalid jump CALCULATED -> DISBURSED rejected")
    void invalidJumpCalculatedToDisbursedRejected() {
        PayrollRun run = PayrollRun.builder()
                .payrollMonth(4)
                .payrollYear(2026)
                .runStatus("CALCULATED")
                .build();
        run.setId(runId);
        run.setCompanyId(tenantId);

        when(payrollRunRepository.findByCompanyIdAndId(tenantId, runId)).thenReturn(Optional.of(run));

        PayrollStepTransitionRequest request = PayrollStepTransitionRequest.builder()
                .targetStatus("DISBURSED")
                .build();

        assertThatThrownBy(() -> unitRunnerService.transitionStep(runId, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot disburse payroll run in status CALCULATED. Expected status: APPROVED");
    }

    @Test
    @DisplayName("State Machine: Invalid jump AUDITED -> DISBURSED rejected")
    void invalidJumpAuditedToDisbursedRejected() {
        PayrollRun run = PayrollRun.builder()
                .payrollMonth(4)
                .payrollYear(2026)
                .runStatus("AUDITED")
                .build();
        run.setId(runId);
        run.setCompanyId(tenantId);

        when(payrollRunRepository.findByCompanyIdAndId(tenantId, runId)).thenReturn(Optional.of(run));

        PayrollStepTransitionRequest request = PayrollStepTransitionRequest.builder()
                .targetStatus("DISBURSED")
                .build();

        assertThatThrownBy(() -> unitRunnerService.transitionStep(runId, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot disburse payroll run in status AUDITED. Expected status: APPROVED");
    }

    @Test
    @DisplayName("State Machine: Attendance lock rejected if monthly attendance summary is unlocked")
    void attendanceLockRejectedIfUnlocked() {
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

        assertThatThrownBy(() -> unitRunnerService.lockAttendance(runId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("is not locked");
    }

    @Test
    @DisplayName("State Machine: Attendance lock rejected if monthly attendance summary is missing")
    void attendanceLockRejectedIfMissing() {
        PayrollRun run = PayrollRun.builder()
                .payrollMonth(4)
                .payrollYear(2026)
                .runStatus("DRAFT")
                .build();
        run.setId(runId);
        run.setCompanyId(tenantId);

        when(payrollRunRepository.findByCompanyIdAndId(tenantId, runId)).thenReturn(Optional.of(run));
        when(monthlyAttendanceSummaryRepository.findByCompanyIdAndPayrollMonthAndPayrollYear(tenantId, 4, 2026))
                .thenReturn(List.of());

        assertThatThrownBy(() -> unitRunnerService.lockAttendance(runId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("has not been generated");
    }

    @Test
    @DisplayName("State Machine: Approval rejected when caller lacks ROLE_FOUNDER or ROLE_FINANCE")
    void approvalRejectedWithoutFounderOrFinanceRole() {
        PayrollRun run = PayrollRun.builder()
                .payrollMonth(4)
                .payrollYear(2026)
                .runStatus("AUDITED")
                .build();
        run.setId(runId);
        run.setCompanyId(tenantId);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("hr@ourhr.com", "pass",
                        List.of(new SimpleGrantedAuthority("ROLE_HR"))));

        when(payrollRunRepository.findByCompanyIdAndId(tenantId, runId)).thenReturn(Optional.of(run));

        assertThatThrownBy(() -> unitRunnerService.approvePayroll(runId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Only users with ROLE_FOUNDER or ROLE_FINANCE can approve payroll runs");
    }

    @Test
    @DisplayName("State Machine: Unsupported step transition status rejected with IllegalArgumentException")
    void unsupportedStepTransitionRejected() {
        PayrollStepTransitionRequest request = PayrollStepTransitionRequest.builder()
                .targetStatus("INVALID_STATUS")
                .build();

        assertThatThrownBy(() -> unitRunnerService.transitionStep(runId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported payroll transition status: INVALID_STATUS");
    }

    // =========================================================================
    // SECTION 4: FULL 5-PERSONA RBAC VERIFICATION ON ALL 8 PAYROLL ENDPOINTS
    // =========================================================================

    // -------------------------------------------------------------------------
    // Endpoint 1: POST /api/v1/payroll/initiate (FOUNDER, HR, FINANCE)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("RBAC: POST /api/v1/payroll/initiate allowed for ROLE_FOUNDER")
    @org.springframework.security.test.context.support.WithMockUser(roles = "FOUNDER")
    void initiatePayroll_allowedForFounder() throws Exception {
        InitiatePayrollRunRequest req = InitiatePayrollRunRequest.builder().payrollMonth(5).payrollYear(2026).build();
        PayrollRunResponse resp = PayrollRunResponse.builder().id(runId).payrollMonth(5).payrollYear(2026).runStatus("DRAFT").build();
        when(mockPayrollRunnerService.initiatePayrollRun(any())).thenReturn(resp);

        mockMvc.perform(post("/api/v1/payroll/initiate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("RBAC: POST /api/v1/payroll/initiate allowed for ROLE_HR")
    @org.springframework.security.test.context.support.WithMockUser(roles = "HR")
    void initiatePayroll_allowedForHr() throws Exception {
        InitiatePayrollRunRequest req = InitiatePayrollRunRequest.builder().payrollMonth(5).payrollYear(2026).build();
        PayrollRunResponse resp = PayrollRunResponse.builder().id(runId).payrollMonth(5).payrollYear(2026).runStatus("DRAFT").build();
        when(mockPayrollRunnerService.initiatePayrollRun(any())).thenReturn(resp);

        mockMvc.perform(post("/api/v1/payroll/initiate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("RBAC: POST /api/v1/payroll/initiate allowed for ROLE_FINANCE")
    @org.springframework.security.test.context.support.WithMockUser(roles = "FINANCE")
    void initiatePayroll_allowedForFinance() throws Exception {
        InitiatePayrollRunRequest req = InitiatePayrollRunRequest.builder().payrollMonth(5).payrollYear(2026).build();
        PayrollRunResponse resp = PayrollRunResponse.builder().id(runId).payrollMonth(5).payrollYear(2026).runStatus("DRAFT").build();
        when(mockPayrollRunnerService.initiatePayrollRun(any())).thenReturn(resp);

        mockMvc.perform(post("/api/v1/payroll/initiate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("RBAC: POST /api/v1/payroll/initiate forbidden for ROLE_CA")
    @org.springframework.security.test.context.support.WithMockUser(roles = "CA")
    void initiatePayroll_forbiddenForCa() throws Exception {
        InitiatePayrollRunRequest req = InitiatePayrollRunRequest.builder().payrollMonth(5).payrollYear(2026).build();

        mockMvc.perform(post("/api/v1/payroll/initiate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("RBAC: POST /api/v1/payroll/initiate forbidden for ROLE_EMPLOYEE")
    @org.springframework.security.test.context.support.WithMockUser(roles = "EMPLOYEE")
    void initiatePayroll_forbiddenForEmployee() throws Exception {
        InitiatePayrollRunRequest req = InitiatePayrollRunRequest.builder().payrollMonth(5).payrollYear(2026).build();

        mockMvc.perform(post("/api/v1/payroll/initiate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("RBAC: POST /api/v1/payroll/initiate unauthorized when unauthenticated")
    void initiatePayroll_unauthorizedWhenAnonymous() throws Exception {
        InitiatePayrollRunRequest req = InitiatePayrollRunRequest.builder().payrollMonth(5).payrollYear(2026).build();

        mockMvc.perform(post("/api/v1/payroll/initiate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------------------
    // Endpoint 2: POST /api/v1/payroll/{id}/step (FOUNDER, HR, FINANCE)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("RBAC: POST /api/v1/payroll/{id}/step allowed for ROLE_FOUNDER")
    @org.springframework.security.test.context.support.WithMockUser(roles = "FOUNDER")
    void stepTransition_allowedForFounder() throws Exception {
        PayrollStepTransitionRequest req = PayrollStepTransitionRequest.builder().targetStatus("ATTENDANCE_LOCKED").build();
        PayrollRunResponse resp = PayrollRunResponse.builder().id(runId).runStatus("ATTENDANCE_LOCKED").build();
        when(mockPayrollRunnerService.transitionStep(eq(runId), any())).thenReturn(resp);

        mockMvc.perform(post("/api/v1/payroll/" + runId + "/step")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("RBAC: POST /api/v1/payroll/{id}/step allowed for ROLE_HR")
    @org.springframework.security.test.context.support.WithMockUser(roles = "HR")
    void stepTransition_allowedForHr() throws Exception {
        PayrollStepTransitionRequest req = PayrollStepTransitionRequest.builder().targetStatus("ADJUSTMENTS_APPLIED").build();
        PayrollRunResponse resp = PayrollRunResponse.builder().id(runId).runStatus("ADJUSTMENTS_APPLIED").build();
        when(mockPayrollRunnerService.transitionStep(eq(runId), any())).thenReturn(resp);

        mockMvc.perform(post("/api/v1/payroll/" + runId + "/step")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("RBAC: POST /api/v1/payroll/{id}/step allowed for ROLE_FINANCE")
    @org.springframework.security.test.context.support.WithMockUser(roles = "FINANCE")
    void stepTransition_allowedForFinance() throws Exception {
        PayrollStepTransitionRequest req = PayrollStepTransitionRequest.builder().targetStatus("APPROVED").build();
        PayrollRunResponse resp = PayrollRunResponse.builder().id(runId).runStatus("APPROVED").build();
        when(mockPayrollRunnerService.transitionStep(eq(runId), any())).thenReturn(resp);

        mockMvc.perform(post("/api/v1/payroll/" + runId + "/step")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("RBAC: POST /api/v1/payroll/{id}/step forbidden for ROLE_CA")
    @org.springframework.security.test.context.support.WithMockUser(roles = "CA")
    void stepTransition_forbiddenForCa() throws Exception {
        PayrollStepTransitionRequest req = PayrollStepTransitionRequest.builder().targetStatus("ATTENDANCE_LOCKED").build();

        mockMvc.perform(post("/api/v1/payroll/" + runId + "/step")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("RBAC: POST /api/v1/payroll/{id}/step forbidden for ROLE_EMPLOYEE")
    @org.springframework.security.test.context.support.WithMockUser(roles = "EMPLOYEE")
    void stepTransition_forbiddenForEmployee() throws Exception {
        PayrollStepTransitionRequest req = PayrollStepTransitionRequest.builder().targetStatus("ATTENDANCE_LOCKED").build();

        mockMvc.perform(post("/api/v1/payroll/" + runId + "/step")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("RBAC: POST /api/v1/payroll/{id}/step unauthorized when unauthenticated")
    void stepTransition_unauthorizedWhenAnonymous() throws Exception {
        PayrollStepTransitionRequest req = PayrollStepTransitionRequest.builder().targetStatus("ATTENDANCE_LOCKED").build();

        mockMvc.perform(post("/api/v1/payroll/" + runId + "/step")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------------------
    // Endpoint 3: POST /api/v1/payroll/{id}/calculate (FOUNDER, HR, FINANCE)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("RBAC: POST /api/v1/payroll/{id}/calculate allowed for ROLE_FOUNDER")
    @org.springframework.security.test.context.support.WithMockUser(roles = "FOUNDER")
    void calculatePayroll_allowedForFounder() throws Exception {
        PayrollRunResponse resp = PayrollRunResponse.builder().id(runId).runStatus("CALCULATED").build();
        when(mockPayrollRunnerService.calculatePayroll(eq(runId))).thenReturn(resp);

        mockMvc.perform(post("/api/v1/payroll/" + runId + "/calculate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("RBAC: POST /api/v1/payroll/{id}/calculate allowed for ROLE_HR")
    @org.springframework.security.test.context.support.WithMockUser(roles = "HR")
    void calculatePayroll_allowedForHr() throws Exception {
        PayrollRunResponse resp = PayrollRunResponse.builder().id(runId).runStatus("CALCULATED").build();
        when(mockPayrollRunnerService.calculatePayroll(eq(runId))).thenReturn(resp);

        mockMvc.perform(post("/api/v1/payroll/" + runId + "/calculate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("RBAC: POST /api/v1/payroll/{id}/calculate allowed for ROLE_FINANCE")
    @org.springframework.security.test.context.support.WithMockUser(roles = "FINANCE")
    void calculatePayroll_allowedForFinance() throws Exception {
        PayrollRunResponse resp = PayrollRunResponse.builder().id(runId).runStatus("CALCULATED").build();
        when(mockPayrollRunnerService.calculatePayroll(eq(runId))).thenReturn(resp);

        mockMvc.perform(post("/api/v1/payroll/" + runId + "/calculate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("RBAC: POST /api/v1/payroll/{id}/calculate forbidden for ROLE_CA")
    @org.springframework.security.test.context.support.WithMockUser(roles = "CA")
    void calculatePayroll_forbiddenForCa() throws Exception {
        mockMvc.perform(post("/api/v1/payroll/" + runId + "/calculate"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("RBAC: POST /api/v1/payroll/{id}/calculate forbidden for ROLE_EMPLOYEE")
    @org.springframework.security.test.context.support.WithMockUser(roles = "EMPLOYEE")
    void calculatePayroll_forbiddenForEmployee() throws Exception {
        mockMvc.perform(post("/api/v1/payroll/" + runId + "/calculate"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("RBAC: POST /api/v1/payroll/{id}/calculate unauthorized when unauthenticated")
    void calculatePayroll_unauthorizedWhenAnonymous() throws Exception {
        mockMvc.perform(post("/api/v1/payroll/" + runId + "/calculate"))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------------------
    // Endpoint 4: GET /api/v1/payroll/{id} (FOUNDER, HR, FINANCE, CA)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("RBAC: GET /api/v1/payroll/{id} allowed for ROLE_FOUNDER")
    @org.springframework.security.test.context.support.WithMockUser(roles = "FOUNDER")
    void getPayrollRun_allowedForFounder() throws Exception {
        PayrollRunResponse resp = PayrollRunResponse.builder().id(runId).runStatus("DRAFT").build();
        when(mockPayrollRunnerService.getPayrollRun(eq(runId))).thenReturn(resp);

        mockMvc.perform(get("/api/v1/payroll/" + runId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("RBAC: GET /api/v1/payroll/{id} allowed for ROLE_HR")
    @org.springframework.security.test.context.support.WithMockUser(roles = "HR")
    void getPayrollRun_allowedForHr() throws Exception {
        PayrollRunResponse resp = PayrollRunResponse.builder().id(runId).runStatus("DRAFT").build();
        when(mockPayrollRunnerService.getPayrollRun(eq(runId))).thenReturn(resp);

        mockMvc.perform(get("/api/v1/payroll/" + runId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("RBAC: GET /api/v1/payroll/{id} allowed for ROLE_FINANCE")
    @org.springframework.security.test.context.support.WithMockUser(roles = "FINANCE")
    void getPayrollRun_allowedForFinance() throws Exception {
        PayrollRunResponse resp = PayrollRunResponse.builder().id(runId).runStatus("DRAFT").build();
        when(mockPayrollRunnerService.getPayrollRun(eq(runId))).thenReturn(resp);

        mockMvc.perform(get("/api/v1/payroll/" + runId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("RBAC: GET /api/v1/payroll/{id} allowed for ROLE_CA")
    @org.springframework.security.test.context.support.WithMockUser(roles = "CA")
    void getPayrollRun_allowedForCa() throws Exception {
        PayrollRunResponse resp = PayrollRunResponse.builder().id(runId).runStatus("DRAFT").build();
        when(mockPayrollRunnerService.getPayrollRun(eq(runId))).thenReturn(resp);

        mockMvc.perform(get("/api/v1/payroll/" + runId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("RBAC: GET /api/v1/payroll/{id} forbidden for ROLE_EMPLOYEE")
    @org.springframework.security.test.context.support.WithMockUser(roles = "EMPLOYEE")
    void getPayrollRun_forbiddenForEmployee() throws Exception {
        mockMvc.perform(get("/api/v1/payroll/" + runId))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("RBAC: GET /api/v1/payroll/{id} unauthorized when unauthenticated")
    void getPayrollRun_unauthorizedWhenAnonymous() throws Exception {
        mockMvc.perform(get("/api/v1/payroll/" + runId))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------------------
    // Endpoint 5: GET /api/v1/payroll/{id}/audit (FOUNDER, HR, FINANCE, CA)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("RBAC: GET /api/v1/payroll/{id}/audit allowed for ROLE_FOUNDER")
    @org.springframework.security.test.context.support.WithMockUser(roles = "FOUNDER")
    void getPayrollAudit_allowedForFounder() throws Exception {
        when(mockPayrollRunnerService.getPayrollAudit(eq(runId))).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/payroll/" + runId + "/audit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("RBAC: GET /api/v1/payroll/{id}/audit allowed for ROLE_HR")
    @org.springframework.security.test.context.support.WithMockUser(roles = "HR")
    void getPayrollAudit_allowedForHr() throws Exception {
        when(mockPayrollRunnerService.getPayrollAudit(eq(runId))).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/payroll/" + runId + "/audit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("RBAC: GET /api/v1/payroll/{id}/audit allowed for ROLE_FINANCE")
    @org.springframework.security.test.context.support.WithMockUser(roles = "FINANCE")
    void getPayrollAudit_allowedForFinance() throws Exception {
        when(mockPayrollRunnerService.getPayrollAudit(eq(runId))).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/payroll/" + runId + "/audit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("RBAC: GET /api/v1/payroll/{id}/audit allowed for ROLE_CA")
    @org.springframework.security.test.context.support.WithMockUser(roles = "CA")
    void getPayrollAudit_allowedForCa() throws Exception {
        when(mockPayrollRunnerService.getPayrollAudit(eq(runId))).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/payroll/" + runId + "/audit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("RBAC: GET /api/v1/payroll/{id}/audit forbidden for ROLE_EMPLOYEE")
    @org.springframework.security.test.context.support.WithMockUser(roles = "EMPLOYEE")
    void getPayrollAudit_forbiddenForEmployee() throws Exception {
        mockMvc.perform(get("/api/v1/payroll/" + runId + "/audit"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("RBAC: GET /api/v1/payroll/{id}/audit unauthorized when unauthenticated")
    void getPayrollAudit_unauthorizedWhenAnonymous() throws Exception {
        mockMvc.perform(get("/api/v1/payroll/" + runId + "/audit"))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------------------
    // Endpoint 6: GET /api/v1/payroll/{id}/payslips/{employeeId} (ALL 5 PERSONAS)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("RBAC: GET /api/v1/payroll/{id}/payslips/{employeeId} allowed for ROLE_FOUNDER")
    @org.springframework.security.test.context.support.WithMockUser(roles = "FOUNDER")
    void getPayslipSummary_allowedForFounder() throws Exception {
        PayslipSummaryDto summary = PayslipSummaryDto.builder()
                .employeeId(employeeId)
                .grossEarnings(new BigDecimal("50000.00"))
                .netPay(new BigDecimal("48000.00"))
                .build();
        when(mockPayrollRunnerService.getPayslipSummary(eq(runId), eq(employeeId))).thenReturn(summary);

        mockMvc.perform(get("/api/v1/payroll/" + runId + "/payslips/" + employeeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("RBAC: GET /api/v1/payroll/{id}/payslips/{employeeId} allowed for ROLE_HR")
    @org.springframework.security.test.context.support.WithMockUser(roles = "HR")
    void getPayslipSummary_allowedForHr() throws Exception {
        PayslipSummaryDto summary = PayslipSummaryDto.builder()
                .employeeId(employeeId)
                .grossEarnings(new BigDecimal("50000.00"))
                .netPay(new BigDecimal("48000.00"))
                .build();
        when(mockPayrollRunnerService.getPayslipSummary(eq(runId), eq(employeeId))).thenReturn(summary);

        mockMvc.perform(get("/api/v1/payroll/" + runId + "/payslips/" + employeeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("RBAC: GET /api/v1/payroll/{id}/payslips/{employeeId} allowed for ROLE_FINANCE")
    @org.springframework.security.test.context.support.WithMockUser(roles = "FINANCE")
    void getPayslipSummary_allowedForFinance() throws Exception {
        PayslipSummaryDto summary = PayslipSummaryDto.builder()
                .employeeId(employeeId)
                .grossEarnings(new BigDecimal("50000.00"))
                .netPay(new BigDecimal("48000.00"))
                .build();
        when(mockPayrollRunnerService.getPayslipSummary(eq(runId), eq(employeeId))).thenReturn(summary);

        mockMvc.perform(get("/api/v1/payroll/" + runId + "/payslips/" + employeeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("RBAC: GET /api/v1/payroll/{id}/payslips/{employeeId} allowed for ROLE_CA")
    @org.springframework.security.test.context.support.WithMockUser(roles = "CA")
    void getPayslipSummary_allowedForCa() throws Exception {
        PayslipSummaryDto summary = PayslipSummaryDto.builder()
                .employeeId(employeeId)
                .grossEarnings(new BigDecimal("50000.00"))
                .netPay(new BigDecimal("48000.00"))
                .build();
        when(mockPayrollRunnerService.getPayslipSummary(eq(runId), eq(employeeId))).thenReturn(summary);

        mockMvc.perform(get("/api/v1/payroll/" + runId + "/payslips/" + employeeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("RBAC: GET /api/v1/payroll/{id}/payslips/{employeeId} allowed for ROLE_EMPLOYEE")
    @org.springframework.security.test.context.support.WithMockUser(roles = "EMPLOYEE")
    void getPayslipSummary_allowedForEmployee() throws Exception {
        PayslipSummaryDto summary = PayslipSummaryDto.builder()
                .employeeId(employeeId)
                .grossEarnings(new BigDecimal("50000.00"))
                .netPay(new BigDecimal("48000.00"))
                .build();
        when(mockPayrollRunnerService.getPayslipSummary(eq(runId), eq(employeeId))).thenReturn(summary);

        mockMvc.perform(get("/api/v1/payroll/" + runId + "/payslips/" + employeeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("RBAC: GET /api/v1/payroll/{id}/payslips/{employeeId} unauthorized when unauthenticated")
    void getPayslipSummary_unauthorizedWhenAnonymous() throws Exception {
        mockMvc.perform(get("/api/v1/payroll/" + runId + "/payslips/" + employeeId))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------------------
    // Endpoint 7: POST /api/v1/payroll/lop-reversals/{id}/approve (FOUNDER, FINANCE)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("RBAC: POST /api/v1/payroll/lop-reversals/{id}/approve allowed for ROLE_FOUNDER")
    @org.springframework.security.test.context.support.WithMockUser(roles = "FOUNDER")
    void approveLopReversal_allowedForFounder() throws Exception {
        UUID revId = UUID.randomUUID();
        LopReversalResponse resp = LopReversalResponse.builder().id(revId).status("APPROVED").build();
        when(mockLopReversalService.approveLopReversal(eq(revId))).thenReturn(resp);

        mockMvc.perform(post("/api/v1/payroll/lop-reversals/" + revId + "/approve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("RBAC: POST /api/v1/payroll/lop-reversals/{id}/approve allowed for ROLE_FINANCE")
    @org.springframework.security.test.context.support.WithMockUser(roles = "FINANCE")
    void approveLopReversal_allowedForFinance() throws Exception {
        UUID revId = UUID.randomUUID();
        LopReversalResponse resp = LopReversalResponse.builder().id(revId).status("APPROVED").build();
        when(mockLopReversalService.approveLopReversal(eq(revId))).thenReturn(resp);

        mockMvc.perform(post("/api/v1/payroll/lop-reversals/" + revId + "/approve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("RBAC: POST /api/v1/payroll/lop-reversals/{id}/approve forbidden for ROLE_HR")
    @org.springframework.security.test.context.support.WithMockUser(roles = "HR")
    void approveLopReversal_forbiddenForHr() throws Exception {
        UUID revId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/payroll/lop-reversals/" + revId + "/approve"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("RBAC: POST /api/v1/payroll/lop-reversals/{id}/approve forbidden for ROLE_CA")
    @org.springframework.security.test.context.support.WithMockUser(roles = "CA")
    void approveLopReversal_forbiddenForCa() throws Exception {
        UUID revId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/payroll/lop-reversals/" + revId + "/approve"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("RBAC: POST /api/v1/payroll/lop-reversals/{id}/approve forbidden for ROLE_EMPLOYEE")
    @org.springframework.security.test.context.support.WithMockUser(roles = "EMPLOYEE")
    void approveLopReversal_forbiddenForEmployee() throws Exception {
        UUID revId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/payroll/lop-reversals/" + revId + "/approve"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("RBAC: POST /api/v1/payroll/lop-reversals/{id}/approve unauthorized when unauthenticated")
    void approveLopReversal_unauthorizedWhenAnonymous() throws Exception {
        UUID revId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/payroll/lop-reversals/" + revId + "/approve"))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------------------
    // Endpoint 8: POST /api/v1/payroll/consultants/payout (FOUNDER, FINANCE)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("RBAC: POST /api/v1/payroll/consultants/payout allowed for ROLE_FOUNDER")
    @org.springframework.security.test.context.support.WithMockUser(roles = "FOUNDER")
    void consultantPayout_allowedForFounder() throws Exception {
        ConsultantPayoutRequest req = ConsultantPayoutRequest.builder()
                .consultantEmployeeId(UUID.randomUUID())
                .grossAmount(new BigDecimal("50000.00"))
                .tdsSection("194J")
                .tdsRate(new BigDecimal("10.0"))
                .build();
        ConsultantPayoutResponse resp = ConsultantPayoutResponse.builder()
                .id(UUID.randomUUID())
                .netPayable(new BigDecimal("45000.00"))
                .build();
        when(mockConsultantPayoutService.processConsultantPayout(any())).thenReturn(resp);

        mockMvc.perform(post("/api/v1/payroll/consultants/payout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("RBAC: POST /api/v1/payroll/consultants/payout allowed for ROLE_FINANCE")
    @org.springframework.security.test.context.support.WithMockUser(roles = "FINANCE")
    void consultantPayout_allowedForFinance() throws Exception {
        ConsultantPayoutRequest req = ConsultantPayoutRequest.builder()
                .consultantEmployeeId(UUID.randomUUID())
                .grossAmount(new BigDecimal("50000.00"))
                .tdsSection("194J")
                .tdsRate(new BigDecimal("10.0"))
                .build();
        ConsultantPayoutResponse resp = ConsultantPayoutResponse.builder()
                .id(UUID.randomUUID())
                .netPayable(new BigDecimal("45000.00"))
                .build();
        when(mockConsultantPayoutService.processConsultantPayout(any())).thenReturn(resp);

        mockMvc.perform(post("/api/v1/payroll/consultants/payout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("RBAC: POST /api/v1/payroll/consultants/payout forbidden for ROLE_HR")
    @org.springframework.security.test.context.support.WithMockUser(roles = "HR")
    void consultantPayout_forbiddenForHr() throws Exception {
        ConsultantPayoutRequest req = ConsultantPayoutRequest.builder()
                .consultantEmployeeId(UUID.randomUUID())
                .grossAmount(new BigDecimal("50000.00"))
                .tdsSection("194J")
                .tdsRate(new BigDecimal("10.0"))
                .build();

        mockMvc.perform(post("/api/v1/payroll/consultants/payout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("RBAC: POST /api/v1/payroll/consultants/payout forbidden for ROLE_CA")
    @org.springframework.security.test.context.support.WithMockUser(roles = "CA")
    void consultantPayout_forbiddenForCa() throws Exception {
        ConsultantPayoutRequest req = ConsultantPayoutRequest.builder()
                .consultantEmployeeId(UUID.randomUUID())
                .grossAmount(new BigDecimal("50000.00"))
                .tdsSection("194J")
                .tdsRate(new BigDecimal("10.0"))
                .build();

        mockMvc.perform(post("/api/v1/payroll/consultants/payout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("RBAC: POST /api/v1/payroll/consultants/payout forbidden for ROLE_EMPLOYEE")
    @org.springframework.security.test.context.support.WithMockUser(roles = "EMPLOYEE")
    void consultantPayout_forbiddenForEmployee() throws Exception {
        ConsultantPayoutRequest req = ConsultantPayoutRequest.builder()
                .consultantEmployeeId(UUID.randomUUID())
                .grossAmount(new BigDecimal("50000.00"))
                .tdsSection("194J")
                .tdsRate(new BigDecimal("10.0"))
                .build();

        mockMvc.perform(post("/api/v1/payroll/consultants/payout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("RBAC: POST /api/v1/payroll/consultants/payout unauthorized when unauthenticated")
    void consultantPayout_unauthorizedWhenAnonymous() throws Exception {
        ConsultantPayoutRequest req = ConsultantPayoutRequest.builder()
                .consultantEmployeeId(UUID.randomUUID())
                .grossAmount(new BigDecimal("50000.00"))
                .tdsSection("194J")
                .tdsRate(new BigDecimal("10.0"))
                .build();

        mockMvc.perform(post("/api/v1/payroll/consultants/payout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }
}

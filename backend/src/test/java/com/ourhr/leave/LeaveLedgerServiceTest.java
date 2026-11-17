package com.ourhr.leave;

import com.ourhr.attendance.HolidayCalendar;
import com.ourhr.attendance.HolidayCalendarRepository;
import com.ourhr.auth.User;
import com.ourhr.auth.UserRepository;
import com.ourhr.common.ResourceNotFoundException;
import com.ourhr.common.TenantContext;
import com.ourhr.company.Branch;
import com.ourhr.company.BranchRepository;
import com.ourhr.employee.Employee;
import com.ourhr.employee.EmployeeRepository;
import com.ourhr.leave.dto.ApplyLeaveRequest;
import com.ourhr.leave.dto.CreateLeaveTypeRequest;
import com.ourhr.leave.dto.LeaveApplicationResponse;
import com.ourhr.leave.dto.LeaveBalanceResponse;
import com.ourhr.leave.dto.LeaveTypeResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaveLedgerServiceTest {

    @Mock
    private LeaveLedgerRepository leaveLedgerRepository;

    @Mock
    private LeaveTypeRepository leaveTypeRepository;

    @Mock
    private LeaveApplicationRepository leaveApplicationRepository;

    @Mock
    private HolidayCalendarRepository holidayCalendarRepository;

    @Mock
    private BranchRepository branchRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private LeaveLedgerService leaveLedgerService;

    private UUID tenantId;
    private UUID employeeId;
    private UUID branchId;
    private UUID leaveTypeId;
    private LeaveType sandwichEnabledLeaveType;
    private LeaveType sandwichDisabledLeaveType;
    private Employee sampleEmployee;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        employeeId = UUID.randomUUID();
        branchId = UUID.randomUUID();
        leaveTypeId = UUID.randomUUID();

        TenantContext.setTenantId(tenantId);

        sandwichEnabledLeaveType = LeaveType.builder()
                .leaveName("Casual Leave")
                .leaveCode("CL")
                .annualQuota(new BigDecimal("12.0"))
                .accrualFrequency("MONTHLY")
                .isCarryForwardAllowed(false)
                .maxCarryForwardLimit(BigDecimal.ZERO)
                .isSandwichRuleEnabled(true)
                .build();
        sandwichEnabledLeaveType.setId(leaveTypeId);
        sandwichEnabledLeaveType.setCompanyId(tenantId);

        sandwichDisabledLeaveType = LeaveType.builder()
                .leaveName("Earned Leave")
                .leaveCode("EL")
                .annualQuota(new BigDecimal("15.0"))
                .accrualFrequency("MONTHLY")
                .isCarryForwardAllowed(true)
                .maxCarryForwardLimit(new BigDecimal("30.0"))
                .isSandwichRuleEnabled(false)
                .build();
        sandwichDisabledLeaveType.setId(UUID.randomUUID());
        sandwichDisabledLeaveType.setCompanyId(tenantId);

        sampleEmployee = Employee.builder()
                .employeeCode("EMP-001")
                .firstName("Aarav")
                .lastName("Sharma")
                .branchId(branchId)
                .employmentStatus("ACTIVE")
                .build();
        sampleEmployee.setId(employeeId);
        sampleEmployee.setCompanyId(tenantId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    // =========================================================================
    // 1. Indian Sandwich Rule Tests
    // =========================================================================

    @Test
    @DisplayName("Sandwich Rule Enabled: Friday to Monday counts 4.0 calendar days (inclusive of Saturday and Sunday)")
    void calculateLeaveDays_sandwichRuleEnabled_includesWeekends() {
        // Friday 2026-10-09 to Monday 2026-10-12
        LocalDate friday = LocalDate.of(2026, 10, 9);
        LocalDate monday = LocalDate.of(2026, 10, 12);

        BigDecimal days = leaveLedgerService.calculateLeaveDays(
                sandwichEnabledLeaveType, branchId, tenantId, friday, monday
        );

        // Friday, Saturday, Sunday, Monday -> 4.0 days
        assertEquals(new BigDecimal("4.0"), days);
    }

    @Test
    @DisplayName("Sandwich Rule Disabled: Friday to Monday counts 2.0 working days (excludes Saturday and Sunday)")
    void calculateLeaveDays_sandwichRuleDisabled_excludesWeekends() {
        // Friday 2026-10-09 to Monday 2026-10-12
        LocalDate friday = LocalDate.of(2026, 10, 9);
        LocalDate monday = LocalDate.of(2026, 10, 12);

        Branch branch = Branch.builder()
                .branchName("Bangalore HQ")
                .city("Bangalore")
                .addressLine("MG Road")
                .pincode("560001")
                .stateCode("KA")
                .build();
        branch.setId(branchId);
        branch.setCompanyId(tenantId);

        when(branchRepository.findByIdAndCompanyId(branchId, tenantId)).thenReturn(Optional.of(branch));
        when(holidayCalendarRepository.findHolidaysBetween(tenantId, "KA", friday, monday)).thenReturn(List.of());

        BigDecimal days = leaveLedgerService.calculateLeaveDays(
                sandwichDisabledLeaveType, branchId, tenantId, friday, monday
        );

        // Friday and Monday -> 2.0 days (Saturday & Sunday excluded)
        assertEquals(new BigDecimal("2.0"), days);
    }

    @Test
    @DisplayName("Sandwich Rule Disabled: Range with intermediate state public holiday excludes both weekend and holiday")
    void calculateLeaveDays_sandwichRuleDisabled_excludesWeekendAndIntermediateHoliday() {
        // Friday 2026-10-09 to Tuesday 2026-10-13
        // Saturday 10-10 & Sunday 10-11 are weekends
        // Monday 10-12 is a declared state public holiday (e.g. Dussehra)
        LocalDate friday = LocalDate.of(2026, 10, 9);
        LocalDate tuesday = LocalDate.of(2026, 10, 13);
        LocalDate mondayHoliday = LocalDate.of(2026, 10, 12);

        Branch branch = Branch.builder()
                .branchName("Mumbai Branch")
                .city("Mumbai")
                .addressLine("BKC")
                .pincode("400051")
                .stateCode("MH")
                .build();
        branch.setId(branchId);
        branch.setCompanyId(tenantId);

        HolidayCalendar holiday = HolidayCalendar.builder()
                .holidayName("Dussehra")
                .holidayDate(mondayHoliday)
                .stateCode("MH")
                .isRestricted(false)
                .build();
        holiday.setId(UUID.randomUUID());
        holiday.setCompanyId(tenantId);

        when(branchRepository.findByIdAndCompanyId(branchId, tenantId)).thenReturn(Optional.of(branch));
        when(holidayCalendarRepository.findHolidaysBetween(tenantId, "MH", friday, tuesday)).thenReturn(List.of(holiday));

        BigDecimal days = leaveLedgerService.calculateLeaveDays(
                sandwichDisabledLeaveType, branchId, tenantId, friday, tuesday
        );

        // Total 5 days: Sat (weekend), Sun (weekend), Mon (holiday) excluded -> only Fri and Tue = 2.0 days
        assertEquals(new BigDecimal("2.0"), days);
    }

    @Test
    @DisplayName("Sandwich Rule Enabled: Range with intermediate public holiday counts ALL 5 calendar days")
    void calculateLeaveDays_sandwichRuleEnabled_includesWeekendAndHoliday() {
        LocalDate friday = LocalDate.of(2026, 10, 9);
        LocalDate tuesday = LocalDate.of(2026, 10, 13);

        BigDecimal days = leaveLedgerService.calculateLeaveDays(
                sandwichEnabledLeaveType, branchId, tenantId, friday, tuesday
        );

        // Friday, Saturday, Sunday, Monday, Tuesday -> 5.0 days
        assertEquals(new BigDecimal("5.0"), days);
    }

    // =========================================================================
    // 2. Immutable Leave Ledger Arithmetic Tests
    // =========================================================================

    @Test
    @DisplayName("Credit Leave: saves ACCRUAL transaction and returns updated balance")
    void creditLeave_savesAccrualAndReturnsBalance() {
        BigDecimal creditDays = new BigDecimal("15.0");
        when(employeeRepository.findByIdAndCompanyId(employeeId, tenantId)).thenReturn(Optional.of(sampleEmployee));
        when(leaveTypeRepository.findByIdAndCompanyId(leaveTypeId, tenantId)).thenReturn(Optional.of(sandwichEnabledLeaveType));
        when(leaveLedgerRepository.getBalance(tenantId, employeeId, leaveTypeId)).thenReturn(creditDays);

        LeaveBalanceResponse response = leaveLedgerService.creditLeave(employeeId, leaveTypeId, creditDays, "Annual opening credit");

        assertNotNull(response);
        assertEquals(leaveTypeId, response.getLeaveTypeId());
        assertEquals("CL", response.getLeaveCode());
        assertEquals(new BigDecimal("15.0"), response.getCreditedDays());
        assertEquals(new BigDecimal("15.0"), response.getRemainingBalance());

        ArgumentCaptor<LeaveLedger> ledgerCaptor = ArgumentCaptor.forClass(LeaveLedger.class);
        verify(leaveLedgerRepository).save(ledgerCaptor.capture());

        LeaveLedger saved = ledgerCaptor.getValue();
        assertEquals(tenantId, saved.getCompanyId());
        assertEquals(employeeId, saved.getEmployeeId());
        assertEquals(leaveTypeId, saved.getLeaveTypeId());
        assertEquals("ACCRUAL", saved.getTransactionType());
        assertEquals(new BigDecimal("15.0"), saved.getDays());
        assertEquals("Annual opening credit", saved.getReferenceNote());
    }

    @Test
    @DisplayName("Credit Leave: throws IllegalArgumentException if credited days is zero or negative")
    void creditLeave_throwsWhenDaysInvalid() {
        when(employeeRepository.findByIdAndCompanyId(employeeId, tenantId)).thenReturn(Optional.of(sampleEmployee));
        when(leaveTypeRepository.findByIdAndCompanyId(leaveTypeId, tenantId)).thenReturn(Optional.of(sandwichEnabledLeaveType));

        assertThrows(IllegalArgumentException.class, () ->
                leaveLedgerService.creditLeave(employeeId, leaveTypeId, BigDecimal.ZERO, "Invalid credit")
        );

        assertThrows(IllegalArgumentException.class, () ->
                leaveLedgerService.creditLeave(employeeId, leaveTypeId, new BigDecimal("-5.0"), "Invalid credit")
        );
    }

    @Test
    @DisplayName("getLeaveBalance: executes SQL sum aggregation and returns exact BigDecimal with scale 1")
    void getLeaveBalance_returnsExactAggregatedBalance() {
        BigDecimal expectedBalance = new BigDecimal("12.5").setScale(1, RoundingMode.HALF_UP);
        when(leaveLedgerRepository.getBalance(tenantId, employeeId, leaveTypeId)).thenReturn(expectedBalance);

        BigDecimal actual = leaveLedgerService.getLeaveBalance(employeeId, leaveTypeId);

        assertEquals(expectedBalance, actual);
        verify(leaveLedgerRepository).getBalance(tenantId, employeeId, leaveTypeId);
    }

    @Test
    @DisplayName("getLeaveBalance: returns 0.0 when repository returns null")
    void getLeaveBalance_returnsZeroWhenNull() {
        when(leaveLedgerRepository.getBalance(tenantId, employeeId, leaveTypeId)).thenReturn(null);

        BigDecimal actual = leaveLedgerService.getLeaveBalance(employeeId, leaveTypeId);

        assertEquals(new BigDecimal("0.0"), actual);
    }

    // =========================================================================
    // 3. Insufficient Balance Validation
    // =========================================================================

    @Test
    @DisplayName("applyLeave: throws IllegalArgumentException when requested days exceed available balance")
    void applyLeave_throwsWhenBalanceInsufficient() {
        // Friday 2026-10-09 to Monday 2026-10-12 with Sandwich = 4.0 days
        LocalDate friday = LocalDate.of(2026, 10, 9);
        LocalDate monday = LocalDate.of(2026, 10, 12);

        ApplyLeaveRequest request = ApplyLeaveRequest.builder()
                .employeeId(employeeId)
                .leaveTypeId(leaveTypeId)
                .startDate(friday)
                .endDate(monday)
                .reason("Personal work")
                .build();

        when(employeeRepository.findByIdAndCompanyId(employeeId, tenantId)).thenReturn(Optional.of(sampleEmployee));
        when(leaveTypeRepository.findByIdAndCompanyId(leaveTypeId, tenantId)).thenReturn(Optional.of(sandwichEnabledLeaveType));
        // Available balance is only 3.0 days, but requested is 4.0 days
        when(leaveLedgerRepository.getBalance(tenantId, employeeId, leaveTypeId)).thenReturn(new BigDecimal("3.0"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                leaveLedgerService.applyLeave(request)
        );

        assertTrue(ex.getMessage().contains("Insufficient leave balance"));
        verify(leaveApplicationRepository, never()).save(any());
    }

    @Test
    @DisplayName("applyLeave: throws IllegalArgumentException when start date is after end date")
    void applyLeave_throwsWhenStartDateAfterEndDate() {
        ApplyLeaveRequest request = ApplyLeaveRequest.builder()
                .employeeId(employeeId)
                .leaveTypeId(leaveTypeId)
                .startDate(LocalDate.of(2026, 10, 15))
                .endDate(LocalDate.of(2026, 10, 10))
                .reason("Invalid dates")
                .build();

        when(employeeRepository.findByIdAndCompanyId(employeeId, tenantId)).thenReturn(Optional.of(sampleEmployee));
        when(leaveTypeRepository.findByIdAndCompanyId(leaveTypeId, tenantId)).thenReturn(Optional.of(sandwichEnabledLeaveType));

        assertThrows(IllegalArgumentException.class, () -> leaveLedgerService.applyLeave(request));
    }

    @Test
    @DisplayName("applyLeave: successfully creates PENDING application when balance is sufficient")
    void applyLeave_createsPendingApplicationWhenSufficient() {
        LocalDate friday = LocalDate.of(2026, 10, 9);
        LocalDate monday = LocalDate.of(2026, 10, 12);

        ApplyLeaveRequest request = ApplyLeaveRequest.builder()
                .employeeId(employeeId)
                .leaveTypeId(leaveTypeId)
                .startDate(friday)
                .endDate(monday)
                .reason("Family function")
                .build();

        when(employeeRepository.findByIdAndCompanyId(employeeId, tenantId)).thenReturn(Optional.of(sampleEmployee));
        when(leaveTypeRepository.findByIdAndCompanyId(leaveTypeId, tenantId)).thenReturn(Optional.of(sandwichEnabledLeaveType));
        // Balance = 10.0 days, requested = 4.0 days
        when(leaveLedgerRepository.getBalance(tenantId, employeeId, leaveTypeId)).thenReturn(new BigDecimal("10.0"));

        UUID appId = UUID.randomUUID();
        when(leaveApplicationRepository.save(any(LeaveApplication.class))).thenAnswer(invocation -> {
            LeaveApplication app = invocation.getArgument(0);
            app.setId(appId);
            return app;
        });

        LeaveApplicationResponse response = leaveLedgerService.applyLeave(request);

        assertNotNull(response);
        assertEquals(appId, response.getId());
        assertEquals("PENDING", response.getStatus());
        assertEquals(new BigDecimal("4.0"), response.getTotalDays());
        assertEquals("CL", response.getLeaveCode());
    }

    // =========================================================================
    // 4. Leave Approval Workflow
    // =========================================================================

    @Test
    @DisplayName("approveLeave: updates status to APPROVED and records negative ledger entry (CONSUMPTION)")
    void approveLeave_updatesStatusAndRecordsLedgerConsumption() {
        UUID applicationId = UUID.randomUUID();
        LeaveApplication application = LeaveApplication.builder()
                .employeeId(employeeId)
                .leaveTypeId(leaveTypeId)
                .startDate(LocalDate.of(2026, 10, 9))
                .endDate(LocalDate.of(2026, 10, 12))
                .totalDays(new BigDecimal("4.0"))
                .status("PENDING")
                .reason("Family function")
                .build();
        application.setId(applicationId);
        application.setCompanyId(tenantId);

        when(leaveApplicationRepository.findByIdAndCompanyId(applicationId, tenantId)).thenReturn(Optional.of(application));
        when(leaveTypeRepository.findByIdAndCompanyId(leaveTypeId, tenantId)).thenReturn(Optional.of(sandwichEnabledLeaveType));
        when(leaveApplicationRepository.save(any(LeaveApplication.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LeaveApplicationResponse response = leaveLedgerService.approveLeave(applicationId);

        assertNotNull(response);
        assertEquals("APPROVED", response.getStatus());
        assertNotNull(response.getActionedAt());

        // Verify ledger record
        ArgumentCaptor<LeaveLedger> ledgerCaptor = ArgumentCaptor.forClass(LeaveLedger.class);
        verify(leaveLedgerRepository).save(ledgerCaptor.capture());

        LeaveLedger ledger = ledgerCaptor.getValue();
        assertEquals(tenantId, ledger.getCompanyId());
        assertEquals(employeeId, ledger.getEmployeeId());
        assertEquals(leaveTypeId, ledger.getLeaveTypeId());
        assertEquals("CONSUMPTION", ledger.getTransactionType());
        // Negative days for consumption: -4.0
        assertEquals(new BigDecimal("-4.0"), ledger.getDays());
        assertEquals(applicationId, ledger.getLeaveApplicationId());
        assertEquals(LocalDate.of(2026, 10, 9), ledger.getEffectiveDate());
    }

    @Test
    @DisplayName("approveLeave: throws IllegalStateException if application is already actioned")
    void approveLeave_throwsWhenNotPending() {
        UUID applicationId = UUID.randomUUID();
        LeaveApplication application = LeaveApplication.builder()
                .employeeId(employeeId)
                .leaveTypeId(leaveTypeId)
                .status("APPROVED")
                .build();
        application.setId(applicationId);
        application.setCompanyId(tenantId);

        when(leaveApplicationRepository.findByIdAndCompanyId(applicationId, tenantId)).thenReturn(Optional.of(application));

        assertThrows(IllegalStateException.class, () -> leaveLedgerService.approveLeave(applicationId));
        verify(leaveLedgerRepository, never()).save(any());
    }

    // =========================================================================
    // 5. Leave Rejection Workflow
    // =========================================================================

    @Test
    @DisplayName("rejectLeave: updates status to REJECTED and leaves ledger unchanged")
    void rejectLeave_updatesStatusAndLeavesLedgerUnchanged() {
        UUID applicationId = UUID.randomUUID();
        LeaveApplication application = LeaveApplication.builder()
                .employeeId(employeeId)
                .leaveTypeId(leaveTypeId)
                .startDate(LocalDate.of(2026, 10, 9))
                .endDate(LocalDate.of(2026, 10, 12))
                .totalDays(new BigDecimal("4.0"))
                .status("PENDING")
                .reason("Vacation")
                .build();
        application.setId(applicationId);
        application.setCompanyId(tenantId);

        when(leaveApplicationRepository.findByIdAndCompanyId(applicationId, tenantId)).thenReturn(Optional.of(application));
        when(leaveTypeRepository.findByIdAndCompanyId(leaveTypeId, tenantId)).thenReturn(Optional.of(sandwichEnabledLeaveType));
        when(leaveApplicationRepository.save(any(LeaveApplication.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LeaveApplicationResponse response = leaveLedgerService.rejectLeave(applicationId, "Critical release scheduled");

        assertNotNull(response);
        assertEquals("REJECTED", response.getStatus());
        assertNotNull(response.getActionedAt());

        // Immutable ledger must NOT have any new entries
        verify(leaveLedgerRepository, never()).save(any());
    }

    @Test
    @DisplayName("rejectLeave: throws IllegalStateException if application is not PENDING")
    void rejectLeave_throwsWhenNotPending() {
        UUID applicationId = UUID.randomUUID();
        LeaveApplication application = LeaveApplication.builder()
                .employeeId(employeeId)
                .leaveTypeId(leaveTypeId)
                .status("REJECTED")
                .build();
        application.setId(applicationId);
        application.setCompanyId(tenantId);

        when(leaveApplicationRepository.findByIdAndCompanyId(applicationId, tenantId)).thenReturn(Optional.of(application));

        assertThrows(IllegalStateException.class, () -> leaveLedgerService.rejectLeave(applicationId, "Duplicate"));
        verify(leaveLedgerRepository, never()).save(any());
    }

    // =========================================================================
    // 6. Leave Types Configuration & Balances
    // =========================================================================

    @Test
    @DisplayName("createLeaveType: saves and returns LeaveTypeResponse")
    void createLeaveType_savesAndReturnsResponse() {
        CreateLeaveTypeRequest request = CreateLeaveTypeRequest.builder()
                .leaveName("Sick Leave")
                .leaveCode("SL")
                .annualQuota(new BigDecimal("10.0"))
                .accrualFrequency("ANNUAL")
                .isCarryForwardAllowed(false)
                .isSandwichRuleEnabled(false)
                .build();

        when(leaveTypeRepository.findByCompanyIdAndLeaveCode(tenantId, "SL")).thenReturn(Optional.empty());
        when(leaveTypeRepository.save(any(LeaveType.class))).thenAnswer(invocation -> {
            LeaveType lt = invocation.getArgument(0);
            lt.setId(UUID.randomUUID());
            return lt;
        });

        LeaveTypeResponse response = leaveLedgerService.createLeaveType(request);

        assertNotNull(response);
        assertEquals("Sick Leave", response.getLeaveName());
        assertEquals("SL", response.getLeaveCode());
        assertEquals(new BigDecimal("10.0"), response.getAnnualQuota());
        assertFalse(response.isSandwichRuleEnabled());
    }

    @Test
    @DisplayName("createLeaveType: throws IllegalArgumentException if duplicate leave code")
    void createLeaveType_throwsWhenDuplicate() {
        CreateLeaveTypeRequest request = CreateLeaveTypeRequest.builder()
                .leaveName("Casual Leave")
                .leaveCode("CL")
                .annualQuota(new BigDecimal("12.0"))
                .accrualFrequency("MONTHLY")
                .build();

        when(leaveTypeRepository.findByCompanyIdAndLeaveCode(tenantId, "CL")).thenReturn(Optional.of(sandwichEnabledLeaveType));

        assertThrows(IllegalArgumentException.class, () -> leaveLedgerService.createLeaveType(request));
    }

    @Test
    @DisplayName("getAllLeaveTypes: returns list for current tenant")
    void getAllLeaveTypes_returnsList() {
        when(leaveTypeRepository.findByCompanyId(tenantId)).thenReturn(List.of(sandwichEnabledLeaveType, sandwichDisabledLeaveType));

        List<LeaveTypeResponse> list = leaveLedgerService.getAllLeaveTypes();

        assertEquals(2, list.size());
    }

    @Test
    @DisplayName("getLeaveBalances: computes credited, consumed, and remaining balance for each leave type")
    void getLeaveBalances_computesAccrualAndConsumption() {
        when(leaveTypeRepository.findByCompanyId(tenantId)).thenReturn(List.of(sandwichEnabledLeaveType));

        LeaveLedger accrual = LeaveLedger.builder()
                .employeeId(employeeId)
                .leaveTypeId(leaveTypeId)
                .transactionType("ACCRUAL")
                .days(new BigDecimal("15.0"))
                .build();

        LeaveLedger consumption = LeaveLedger.builder()
                .employeeId(employeeId)
                .leaveTypeId(leaveTypeId)
                .transactionType("CONSUMPTION")
                .days(new BigDecimal("-3.0"))
                .build();

        when(leaveLedgerRepository.findByCompanyIdAndEmployeeIdAndLeaveTypeIdOrderByCreatedAtDesc(tenantId, employeeId, leaveTypeId))
                .thenReturn(List.of(consumption, accrual));

        List<LeaveBalanceResponse> balances = leaveLedgerService.getLeaveBalances(employeeId);

        assertEquals(1, balances.size());
        LeaveBalanceResponse b = balances.get(0);
        assertEquals("CL", b.getLeaveCode());
        assertEquals(new BigDecimal("15.0"), b.getCreditedDays());
        assertEquals(new BigDecimal("3.0"), b.getConsumedDays());
        assertEquals(new BigDecimal("12.0"), b.getRemainingBalance());
    }

    @Test
    @DisplayName("Service throws IllegalArgumentException when tenant context is missing")
    void throwsWhenTenantContextMissing() {
        TenantContext.clear();

        assertThrows(IllegalArgumentException.class, () -> leaveLedgerService.getAllLeaveTypes());
        assertThrows(IllegalArgumentException.class, () -> leaveLedgerService.getLeaveBalance(employeeId, leaveTypeId));
    }
}

package com.ourhr.leave;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ourhr.leave.dto.ApplyLeaveRequest;
import com.ourhr.leave.dto.CreateLeaveTypeRequest;
import com.ourhr.leave.dto.CreditLeaveRequest;
import com.ourhr.leave.dto.LeaveApplicationResponse;
import com.ourhr.leave.dto.LeaveBalanceResponse;
import com.ourhr.leave.dto.LeaveTypeResponse;
import com.ourhr.leave.dto.RejectLeaveRequest;
import com.ourhr.security.JwtAuthenticationFilter;
import com.ourhr.security.JwtService;
import com.ourhr.security.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LeaveController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class LeaveControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LeaveLedgerService leaveLedgerService;

    @MockBean
    private JwtService jwtService;

    private CreateLeaveTypeRequest buildCreateTypeRequest() {
        return CreateLeaveTypeRequest.builder()
                .leaveName("Casual Leave")
                .leaveCode("CL")
                .annualQuota(new BigDecimal("12.0"))
                .accrualFrequency("MONTHLY")
                .isCarryForwardAllowed(false)
                .isSandwichRuleEnabled(true)
                .build();
    }

    private LeaveTypeResponse buildLeaveTypeResponse(UUID id) {
        return LeaveTypeResponse.builder()
                .id(id)
                .leaveName("Casual Leave")
                .leaveCode("CL")
                .annualQuota(new BigDecimal("12.0"))
                .accrualFrequency("MONTHLY")
                .isCarryForwardAllowed(false)
                .isSandwichRuleEnabled(true)
                .build();
    }

    private ApplyLeaveRequest buildApplyRequest(UUID employeeId, UUID leaveTypeId) {
        return ApplyLeaveRequest.builder()
                .employeeId(employeeId)
                .leaveTypeId(leaveTypeId)
                .startDate(LocalDate.of(2026, 10, 9))
                .endDate(LocalDate.of(2026, 10, 12))
                .reason("Personal work")
                .build();
    }

    private LeaveApplicationResponse buildApplicationResponse(UUID id, String status) {
        return LeaveApplicationResponse.builder()
                .id(id)
                .employeeId(UUID.randomUUID())
                .leaveTypeId(UUID.randomUUID())
                .leaveCode("CL")
                .leaveName("Casual Leave")
                .startDate(LocalDate.of(2026, 10, 9))
                .endDate(LocalDate.of(2026, 10, 12))
                .totalDays(new BigDecimal("4.0"))
                .status(status)
                .build();
    }

    // =========================================================================
    // POST /api/v1/leaves/types RBAC
    // =========================================================================

    @Test
    @DisplayName("POST /api/v1/leaves/types returns 201 for ROLE_HR")
    @WithMockUser(roles = "HR")
    void createLeaveType_allowedForHr() throws Exception {
        UUID id = UUID.randomUUID();
        when(leaveLedgerService.createLeaveType(any())).thenReturn(buildLeaveTypeResponse(id));

        mockMvc.perform(post("/api/v1/leaves/types")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildCreateTypeRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.leaveCode").value("CL"));
    }

    @Test
    @DisplayName("POST /api/v1/leaves/types returns 201 for ROLE_FOUNDER")
    @WithMockUser(roles = "FOUNDER")
    void createLeaveType_allowedForFounder() throws Exception {
        UUID id = UUID.randomUUID();
        when(leaveLedgerService.createLeaveType(any())).thenReturn(buildLeaveTypeResponse(id));

        mockMvc.perform(post("/api/v1/leaves/types")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildCreateTypeRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("POST /api/v1/leaves/types returns 403 for ROLE_EMPLOYEE")
    @WithMockUser(roles = "EMPLOYEE")
    void createLeaveType_forbiddenForEmployee() throws Exception {
        mockMvc.perform(post("/api/v1/leaves/types")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildCreateTypeRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/leaves/types returns 403 for ROLE_FINANCE")
    @WithMockUser(roles = "FINANCE")
    void createLeaveType_forbiddenForFinance() throws Exception {
        mockMvc.perform(post("/api/v1/leaves/types")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildCreateTypeRequest())))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // GET /api/v1/leaves/types RBAC
    // =========================================================================

    @Test
    @DisplayName("GET /api/v1/leaves/types returns 200 for ROLE_EMPLOYEE")
    @WithMockUser(roles = "EMPLOYEE")
    void getAllLeaveTypes_allowedForEmployee() throws Exception {
        when(leaveLedgerService.getAllLeaveTypes()).thenReturn(List.of(buildLeaveTypeResponse(UUID.randomUUID())));

        mockMvc.perform(get("/api/v1/leaves/types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("GET /api/v1/leaves/types returns 403 for ROLE_FINANCE")
    @WithMockUser(roles = "FINANCE")
    void getAllLeaveTypes_forbiddenForFinance() throws Exception {
        mockMvc.perform(get("/api/v1/leaves/types"))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // POST /api/v1/leaves/apply RBAC
    // =========================================================================

    @Test
    @DisplayName("POST /api/v1/leaves/apply returns 201 for ROLE_EMPLOYEE")
    @WithMockUser(roles = "EMPLOYEE")
    void applyLeave_allowedForEmployee() throws Exception {
        UUID id = UUID.randomUUID();
        when(leaveLedgerService.applyLeave(any())).thenReturn(buildApplicationResponse(id, "PENDING"));

        mockMvc.perform(post("/api/v1/leaves/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildApplyRequest(UUID.randomUUID(), UUID.randomUUID()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.totalDays").value(4.0));
    }

    @Test
    @DisplayName("POST /api/v1/leaves/apply returns 403 for ROLE_FINANCE")
    @WithMockUser(roles = "FINANCE")
    void applyLeave_forbiddenForFinance() throws Exception {
        mockMvc.perform(post("/api/v1/leaves/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildApplyRequest(UUID.randomUUID(), UUID.randomUUID()))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/leaves/apply returns 403 for ROLE_CA")
    @WithMockUser(roles = "CA")
    void applyLeave_forbiddenForCa() throws Exception {
        mockMvc.perform(post("/api/v1/leaves/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildApplyRequest(UUID.randomUUID(), UUID.randomUUID()))))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // POST /api/v1/leaves/{id}/approve RBAC
    // =========================================================================

    @Test
    @DisplayName("POST /api/v1/leaves/{id}/approve returns 200 for ROLE_HR")
    @WithMockUser(roles = "HR")
    void approveLeave_allowedForHr() throws Exception {
        UUID id = UUID.randomUUID();
        when(leaveLedgerService.approveLeave(id)).thenReturn(buildApplicationResponse(id, "APPROVED"));

        mockMvc.perform(post("/api/v1/leaves/" + id + "/approve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("APPROVED"));
    }

    @Test
    @DisplayName("POST /api/v1/leaves/{id}/approve returns 200 for ROLE_FOUNDER")
    @WithMockUser(roles = "FOUNDER")
    void approveLeave_allowedForFounder() throws Exception {
        UUID id = UUID.randomUUID();
        when(leaveLedgerService.approveLeave(id)).thenReturn(buildApplicationResponse(id, "APPROVED"));

        mockMvc.perform(post("/api/v1/leaves/" + id + "/approve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("POST /api/v1/leaves/{id}/approve returns 403 for ROLE_EMPLOYEE")
    @WithMockUser(roles = "EMPLOYEE")
    void approveLeave_forbiddenForEmployee() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(post("/api/v1/leaves/" + id + "/approve"))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // POST /api/v1/leaves/{id}/reject RBAC
    // =========================================================================

    @Test
    @DisplayName("POST /api/v1/leaves/{id}/reject returns 200 for ROLE_HR")
    @WithMockUser(roles = "HR")
    void rejectLeave_allowedForHr() throws Exception {
        UUID id = UUID.randomUUID();
        when(leaveLedgerService.rejectLeave(eq(id), any())).thenReturn(buildApplicationResponse(id, "REJECTED"));

        RejectLeaveRequest request = RejectLeaveRequest.builder().reason("Staff shortage").build();

        mockMvc.perform(post("/api/v1/leaves/" + id + "/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("REJECTED"));
    }

    @Test
    @DisplayName("POST /api/v1/leaves/{id}/reject returns 403 for ROLE_EMPLOYEE")
    @WithMockUser(roles = "EMPLOYEE")
    void rejectLeave_forbiddenForEmployee() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(post("/api/v1/leaves/" + id + "/reject"))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // GET /api/v1/leaves/balances/{employeeId} RBAC
    // =========================================================================

    @Test
    @DisplayName("GET /api/v1/leaves/balances/{employeeId} returns 200 for ROLE_EMPLOYEE")
    @WithMockUser(roles = "EMPLOYEE")
    void getLeaveBalances_allowedForEmployee() throws Exception {
        UUID empId = UUID.randomUUID();
        LeaveBalanceResponse bal = LeaveBalanceResponse.builder()
                .leaveTypeId(UUID.randomUUID())
                .leaveCode("CL")
                .leaveName("Casual Leave")
                .creditedDays(new BigDecimal("15.0"))
                .consumedDays(new BigDecimal("3.0"))
                .remainingBalance(new BigDecimal("12.0"))
                .build();

        when(leaveLedgerService.getLeaveBalances(empId)).thenReturn(List.of(bal));

        mockMvc.perform(get("/api/v1/leaves/balances/" + empId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].remainingBalance").value(12.0));
    }

    @Test
    @DisplayName("GET /api/v1/leaves/balances/{employeeId} returns 403 for ROLE_FINANCE")
    @WithMockUser(roles = "FINANCE")
    void getLeaveBalances_forbiddenForFinance() throws Exception {
        UUID empId = UUID.randomUUID();
        mockMvc.perform(get("/api/v1/leaves/balances/" + empId))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // POST /api/v1/leaves/credit RBAC
    // =========================================================================

    @Test
    @DisplayName("POST /api/v1/leaves/credit returns 200 for ROLE_HR")
    @WithMockUser(roles = "HR")
    void creditLeave_allowedForHr() throws Exception {
        UUID empId = UUID.randomUUID();
        UUID leaveTypeId = UUID.randomUUID();

        CreditLeaveRequest request = CreditLeaveRequest.builder()
                .employeeId(empId)
                .leaveTypeId(leaveTypeId)
                .days(new BigDecimal("15.0"))
                .note("Annual opening credit")
                .build();

        LeaveBalanceResponse bal = LeaveBalanceResponse.builder()
                .leaveTypeId(leaveTypeId)
                .leaveCode("CL")
                .leaveName("Casual Leave")
                .creditedDays(new BigDecimal("15.0"))
                .consumedDays(BigDecimal.ZERO)
                .remainingBalance(new BigDecimal("15.0"))
                .build();

        when(leaveLedgerService.creditLeave(eq(empId), eq(leaveTypeId), any(), any())).thenReturn(bal);

        mockMvc.perform(post("/api/v1/leaves/credit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.remainingBalance").value(15.0));
    }

    @Test
    @DisplayName("POST /api/v1/leaves/credit returns 403 for ROLE_EMPLOYEE")
    @WithMockUser(roles = "EMPLOYEE")
    void creditLeave_forbiddenForEmployee() throws Exception {
        CreditLeaveRequest request = CreditLeaveRequest.builder()
                .employeeId(UUID.randomUUID())
                .leaveTypeId(UUID.randomUUID())
                .days(new BigDecimal("15.0"))
                .build();

        mockMvc.perform(post("/api/v1/leaves/credit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // GET /api/v1/leaves/applications RBAC
    // =========================================================================

    @Test
    @DisplayName("GET /api/v1/leaves/applications returns 200 for ROLE_EMPLOYEE")
    @WithMockUser(roles = "EMPLOYEE")
    void getLeaveApplications_allowedForEmployee() throws Exception {
        when(leaveLedgerService.getLeaveApplications(any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/leaves/applications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("GET /api/v1/leaves/applications returns 403 for ROLE_FINANCE")
    @WithMockUser(roles = "FINANCE")
    void getLeaveApplications_forbiddenForFinance() throws Exception {
        mockMvc.perform(get("/api/v1/leaves/applications"))
                .andExpect(status().isForbidden());
    }
}

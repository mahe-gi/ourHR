package com.ourhr.payroll;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ourhr.payroll.dto.InitiatePayrollRunRequest;
import com.ourhr.payroll.dto.PayrollAuditDiffResponse;
import com.ourhr.payroll.dto.PayrollRunResponse;
import com.ourhr.payroll.dto.PayrollStepTransitionRequest;
import com.ourhr.payroll.dto.PayslipSummaryDto;
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
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PayrollController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class PayrollControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PayrollRunnerService payrollRunnerService;

    @MockBean
    private JwtService jwtService;

    @Test
    @DisplayName("FOUNDER should successfully initiate payroll run")
    @WithMockUser(roles = "FOUNDER")
    void founderShouldInitiatePayrollRun() throws Exception {
        InitiatePayrollRunRequest request = InitiatePayrollRunRequest.builder()
                .payrollMonth(4)
                .payrollYear(2026)
                .build();

        UUID runId = UUID.randomUUID();
        PayrollRunResponse response = PayrollRunResponse.builder()
                .id(runId)
                .payrollMonth(4)
                .payrollYear(2026)
                .runStatus("DRAFT")
                .build();

        when(payrollRunnerService.initiatePayrollRun(any(InitiatePayrollRunRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/payroll/initiate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(runId.toString()))
                .andExpect(jsonPath("$.data.runStatus").value("DRAFT"));
    }

    @Test
    @DisplayName("EMPLOYEE should be forbidden from initiating payroll run")
    @WithMockUser(roles = "EMPLOYEE")
    void employeeForbiddenFromInitiating() throws Exception {
        InitiatePayrollRunRequest request = InitiatePayrollRunRequest.builder()
                .payrollMonth(4)
                .payrollYear(2026)
                .build();

        mockMvc.perform(post("/api/v1/payroll/initiate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("FINANCE should trigger step transition")
    @WithMockUser(roles = "FINANCE")
    void financeShouldTriggerStepTransition() throws Exception {
        UUID runId = UUID.randomUUID();
        PayrollStepTransitionRequest request = PayrollStepTransitionRequest.builder()
                .targetStatus("ATTENDANCE_LOCKED")
                .build();

        PayrollRunResponse response = PayrollRunResponse.builder()
                .id(runId)
                .runStatus("ATTENDANCE_LOCKED")
                .build();

        when(payrollRunnerService.transitionStep(eq(runId), any(PayrollStepTransitionRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/payroll/" + runId + "/step")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.runStatus").value("ATTENDANCE_LOCKED"));
    }

    @Test
    @DisplayName("HR should trigger calculation")
    @WithMockUser(roles = "HR")
    void hrShouldTriggerCalculation() throws Exception {
        UUID runId = UUID.randomUUID();
        PayrollRunResponse response = PayrollRunResponse.builder()
                .id(runId)
                .runStatus("CALCULATED")
                .totalGrossPay(new BigDecimal("500000.00"))
                .build();

        when(payrollRunnerService.calculatePayroll(eq(runId))).thenReturn(response);

        mockMvc.perform(post("/api/v1/payroll/" + runId + "/calculate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.runStatus").value("CALCULATED"));
    }

    @Test
    @DisplayName("CA should view audit diffs")
    @WithMockUser(roles = "CA")
    void caShouldViewAudit() throws Exception {
        UUID runId = UUID.randomUUID();
        PayrollAuditDiffResponse diff = PayrollAuditDiffResponse.builder()
                .employeeCode("EMP001")
                .employeeName("Mahesh Ch")
                .previousNetPay(new BigDecimal("40000.00"))
                .currentNetPay(new BigDecimal("45000.00"))
                .netPayDifference(new BigDecimal("5000.00"))
                .auditFlagReason("SALARY_INCREASE")
                .build();

        when(payrollRunnerService.getPayrollAudit(eq(runId))).thenReturn(List.of(diff));

        mockMvc.perform(get("/api/v1/payroll/" + runId + "/audit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].employeeCode").value("EMP001"))
                .andExpect(jsonPath("$.data[0].auditFlagReason").value("SALARY_INCREASE"));
    }

    @Test
    @DisplayName("EMPLOYEE should view payslip summary")
    @WithMockUser(roles = "EMPLOYEE")
    void employeeShouldViewPayslipSummary() throws Exception {
        UUID runId = UUID.randomUUID();
        UUID empId = UUID.randomUUID();

        PayslipSummaryDto summary = PayslipSummaryDto.builder()
                .employeeId(empId)
                .employeeCode("EMP001")
                .employeeName("Mahesh Ch")
                .grossEarnings(new BigDecimal("50000.00"))
                .totalDeductions(new BigDecimal("2000.00"))
                .netPay(new BigDecimal("48000.00"))
                .build();

        when(payrollRunnerService.getPayslipSummary(eq(runId), eq(empId))).thenReturn(summary);

        mockMvc.perform(get("/api/v1/payroll/" + runId + "/payslips/" + empId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.employeeName").value("Mahesh Ch"))
                .andExpect(jsonPath("$.data.netPay").value(48000.00));
    }
}

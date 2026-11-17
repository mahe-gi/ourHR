package com.ourhr.attendance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ourhr.attendance.dto.FreezeAttendanceRequest;
import com.ourhr.attendance.dto.MonthlyAttendanceSummaryResponse;
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
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AttendanceFreezeController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class AttendanceFreezeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AttendanceFreezeService attendanceFreezeService;

    @MockBean
    private JwtService jwtService;

    private FreezeAttendanceRequest buildRequest() {
        return FreezeAttendanceRequest.builder()
                .payrollMonth(10)
                .payrollYear(2026)
                .build();
    }

    private MonthlyAttendanceSummaryResponse buildResponse() {
        return MonthlyAttendanceSummaryResponse.builder()
                .id(UUID.randomUUID())
                .employeeId(UUID.randomUUID())
                .employeeCode("EMP-101")
                .employeeName("Aditi Deshmukh")
                .payrollMonth(10)
                .payrollYear(2026)
                .totalCalendarDays(31)
                .payableDays(new BigDecimal("31.0"))
                .lopDays(new BigDecimal("0.0"))
                .isLocked(true)
                .lockedAt(Instant.now())
                .build();
    }

    // =========================================================================
    // POST /api/v1/attendance/freeze RBAC
    // =========================================================================

    @Test
    @DisplayName("POST /api/v1/attendance/freeze returns 200 for ROLE_FINANCE")
    @WithMockUser(roles = "FINANCE")
    void freezeAttendance_allowedForFinance() throws Exception {
        when(attendanceFreezeService.freezeMonthlyAttendance(any())).thenReturn(List.of(buildResponse()));

        mockMvc.perform(post("/api/v1/attendance/freeze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].employeeCode").value("EMP-101"))
                .andExpect(jsonPath("$.data[0].locked").value(true));
    }

    @Test
    @DisplayName("POST /api/v1/attendance/freeze returns 200 for ROLE_HR")
    @WithMockUser(roles = "HR")
    void freezeAttendance_allowedForHr() throws Exception {
        when(attendanceFreezeService.freezeMonthlyAttendance(any())).thenReturn(List.of(buildResponse()));

        mockMvc.perform(post("/api/v1/attendance/freeze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("POST /api/v1/attendance/freeze returns 200 for ROLE_FOUNDER")
    @WithMockUser(roles = "FOUNDER")
    void freezeAttendance_allowedForFounder() throws Exception {
        when(attendanceFreezeService.freezeMonthlyAttendance(any())).thenReturn(List.of(buildResponse()));

        mockMvc.perform(post("/api/v1/attendance/freeze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("POST /api/v1/attendance/freeze returns 403 for ROLE_EMPLOYEE")
    @WithMockUser(roles = "EMPLOYEE")
    void freezeAttendance_forbiddenForEmployee() throws Exception {
        mockMvc.perform(post("/api/v1/attendance/freeze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/attendance/freeze returns 403 for ROLE_CA")
    @WithMockUser(roles = "CA")
    void freezeAttendance_forbiddenForCa() throws Exception {
        mockMvc.perform(post("/api/v1/attendance/freeze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/attendance/freeze returns 403 when unauthenticated")
    void freezeAttendance_unauthorizedWhenNoAuth() throws Exception {
        mockMvc.perform(post("/api/v1/attendance/freeze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // GET /api/v1/attendance/freeze/summary RBAC
    // =========================================================================

    @Test
    @DisplayName("GET /api/v1/attendance/freeze/summary returns 200 for ROLE_CA")
    @WithMockUser(roles = "CA")
    void getMonthlySummary_allowedForCa() throws Exception {
        when(attendanceFreezeService.getMonthlySummary(10, 2026)).thenReturn(List.of(buildResponse()));

        mockMvc.perform(get("/api/v1/attendance/freeze/summary")
                        .param("payrollMonth", "10")
                        .param("payrollYear", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].employeeCode").value("EMP-101"));
    }

    @Test
    @DisplayName("GET /api/v1/attendance/freeze/summary returns 200 for ROLE_FINANCE")
    @WithMockUser(roles = "FINANCE")
    void getMonthlySummary_allowedForFinance() throws Exception {
        when(attendanceFreezeService.getMonthlySummary(10, 2026)).thenReturn(List.of(buildResponse()));

        mockMvc.perform(get("/api/v1/attendance/freeze/summary")
                        .param("payrollMonth", "10")
                        .param("payrollYear", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("GET /api/v1/attendance/freeze/summary returns 403 for ROLE_EMPLOYEE")
    @WithMockUser(roles = "EMPLOYEE")
    void getMonthlySummary_forbiddenForEmployee() throws Exception {
        mockMvc.perform(get("/api/v1/attendance/freeze/summary")
                        .param("payrollMonth", "10")
                        .param("payrollYear", "2026"))
                .andExpect(status().isForbidden());
    }
}

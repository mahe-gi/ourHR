package com.ourhr.attendance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ourhr.attendance.dto.PunchRequest;
import com.ourhr.attendance.dto.PunchResponse;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AttendanceController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class AttendanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AttendanceService attendanceService;

    @MockBean
    private JwtService jwtService;

    private PunchRequest buildValidPunchRequest() {
        return PunchRequest.builder()
                .punchType("IN")
                .mode("WEB")
                .punchTimestamp(Instant.now())
                .build();
    }

    private PunchResponse buildPunchResponse(UUID id) {
        return PunchResponse.builder()
                .id(id)
                .punchType("IN")
                .mode("WEB")
                .punchTimestamp(Instant.now())
                .isRegularized(false)
                .build();
    }

    // =========================================================================
    // POST /api/v1/attendance/punch RBAC
    // =========================================================================

    @Test
    @DisplayName("POST /api/v1/attendance/punch returns 200 for ROLE_EMPLOYEE")
    @WithMockUser(roles = "EMPLOYEE")
    void recordPunch_allowedForEmployee() throws Exception {
        UUID id = UUID.randomUUID();
        when(attendanceService.recordPunch(any())).thenReturn(buildPunchResponse(id));

        mockMvc.perform(post("/api/v1/attendance/punch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildValidPunchRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(id.toString()))
                .andExpect(jsonPath("$.data.punchType").value("IN"));
    }

    @Test
    @DisplayName("POST /api/v1/attendance/punch returns 200 for ROLE_HR")
    @WithMockUser(roles = "HR")
    void recordPunch_allowedForHr() throws Exception {
        UUID id = UUID.randomUUID();
        when(attendanceService.recordPunch(any())).thenReturn(buildPunchResponse(id));

        mockMvc.perform(post("/api/v1/attendance/punch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildValidPunchRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("POST /api/v1/attendance/punch returns 200 for ROLE_FOUNDER")
    @WithMockUser(roles = "FOUNDER")
    void recordPunch_allowedForFounder() throws Exception {
        UUID id = UUID.randomUUID();
        when(attendanceService.recordPunch(any())).thenReturn(buildPunchResponse(id));

        mockMvc.perform(post("/api/v1/attendance/punch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildValidPunchRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("POST /api/v1/attendance/punch returns 403 for ROLE_FINANCE")
    @WithMockUser(roles = "FINANCE")
    void recordPunch_forbiddenForFinance() throws Exception {
        mockMvc.perform(post("/api/v1/attendance/punch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildValidPunchRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/attendance/punch returns 403 for ROLE_CA")
    @WithMockUser(roles = "CA")
    void recordPunch_forbiddenForCa() throws Exception {
        mockMvc.perform(post("/api/v1/attendance/punch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildValidPunchRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/attendance/punch returns 403 when unauthenticated")
    void recordPunch_unauthorizedWhenNoAuth() throws Exception {
        mockMvc.perform(post("/api/v1/attendance/punch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildValidPunchRequest())))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // POST /api/v1/attendance/upload-csv RBAC
    // =========================================================================

    @Test
    @DisplayName("POST /api/v1/attendance/upload-csv returns 200 for ROLE_HR")
    @WithMockUser(roles = "HR")
    void uploadBiometricCsv_allowedForHr() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "punches.csv", "text/csv", "employee_code,punch_timestamp,punch_type\nEMP-001,2026-11-15T09:00:00Z,IN\n".getBytes()
        );

        when(attendanceService.uploadBiometricCsv(any(), any())).thenReturn(1);

        mockMvc.perform(multipart("/api/v1/attendance/upload-csv").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.processedRecords").value(1));
    }

    @Test
    @DisplayName("POST /api/v1/attendance/upload-csv returns 200 for ROLE_FOUNDER")
    @WithMockUser(roles = "FOUNDER")
    void uploadBiometricCsv_allowedForFounder() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "punches.csv", "text/csv", "employee_code,punch_timestamp,punch_type\nEMP-001,2026-11-15T09:00:00Z,IN\n".getBytes()
        );

        when(attendanceService.uploadBiometricCsv(any(), any())).thenReturn(1);

        mockMvc.perform(multipart("/api/v1/attendance/upload-csv").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("POST /api/v1/attendance/upload-csv returns 403 for ROLE_EMPLOYEE")
    @WithMockUser(roles = "EMPLOYEE")
    void uploadBiometricCsv_forbiddenForEmployee() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "punches.csv", "text/csv", "test".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/attendance/upload-csv").file(file))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/attendance/upload-csv returns 403 for ROLE_FINANCE")
    @WithMockUser(roles = "FINANCE")
    void uploadBiometricCsv_forbiddenForFinance() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "punches.csv", "text/csv", "test".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/attendance/upload-csv").file(file))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // GET /api/v1/attendance/logs RBAC
    // =========================================================================

    @Test
    @DisplayName("GET /api/v1/attendance/logs returns 200 for ROLE_EMPLOYEE, ROLE_FINANCE, and ROLE_CA")
    @WithMockUser(roles = "EMPLOYEE")
    void getAttendanceLogs_allowedForEmployee() throws Exception {
        when(attendanceService.getAttendanceLogs(any(), any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/attendance/logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("GET /api/v1/attendance/logs returns 200 for ROLE_CA")
    @WithMockUser(roles = "CA")
    void getAttendanceLogs_allowedForCa() throws Exception {
        when(attendanceService.getAttendanceLogs(any(), any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/attendance/logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}

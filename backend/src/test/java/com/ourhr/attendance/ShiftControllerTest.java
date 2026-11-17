package com.ourhr.attendance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ourhr.attendance.dto.CreateShiftRequest;
import com.ourhr.attendance.dto.ShiftResponse;
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
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ShiftController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class ShiftControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ShiftService shiftService;

    @MockBean
    private JwtService jwtService;

    private CreateShiftRequest buildValidRequest() {
        return CreateShiftRequest.builder()
                .shiftName("General Shift")
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(18, 0))
                .graceMinutes(15)
                .halfDayThresholdHours(new BigDecimal("4.50"))
                .fullDayThresholdHours(new BigDecimal("8.00"))
                .lateMarkPenaltyCount(3)
                .lateMarkPenaltyDeductionDays(new BigDecimal("0.5"))
                .build();
    }

    private ShiftResponse buildResponse(UUID id) {
        return ShiftResponse.builder()
                .id(id)
                .shiftName("General Shift")
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(18, 0))
                .graceMinutes(15)
                .halfDayThresholdHours(new BigDecimal("4.50"))
                .fullDayThresholdHours(new BigDecimal("8.00"))
                .lateMarkPenaltyCount(3)
                .lateMarkPenaltyDeductionDays(new BigDecimal("0.5"))
                .build();
    }

    // =========================================================================
    // POST /api/v1/shifts RBAC
    // =========================================================================

    @Test
    @DisplayName("POST /api/v1/shifts returns 201 for ROLE_HR")
    @WithMockUser(roles = "HR")
    void createShift_allowedForHr() throws Exception {
        UUID id = UUID.randomUUID();
        when(shiftService.createShift(any())).thenReturn(buildResponse(id));

        mockMvc.perform(post("/api/v1/shifts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildValidRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.shiftName").value("General Shift"));
    }

    @Test
    @DisplayName("POST /api/v1/shifts returns 201 for ROLE_FOUNDER")
    @WithMockUser(roles = "FOUNDER")
    void createShift_allowedForFounder() throws Exception {
        UUID id = UUID.randomUUID();
        when(shiftService.createShift(any())).thenReturn(buildResponse(id));

        mockMvc.perform(post("/api/v1/shifts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildValidRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("POST /api/v1/shifts returns 403 for ROLE_EMPLOYEE")
    @WithMockUser(roles = "EMPLOYEE")
    void createShift_forbiddenForEmployee() throws Exception {
        mockMvc.perform(post("/api/v1/shifts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildValidRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/shifts returns 403 for ROLE_FINANCE")
    @WithMockUser(roles = "FINANCE")
    void createShift_forbiddenForFinance() throws Exception {
        mockMvc.perform(post("/api/v1/shifts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildValidRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/shifts returns 403 for ROLE_CA")
    @WithMockUser(roles = "CA")
    void createShift_forbiddenForCa() throws Exception {
        mockMvc.perform(post("/api/v1/shifts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildValidRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/shifts returns 403 when unauthenticated")
    void createShift_unauthorizedWhenNoAuth() throws Exception {
        mockMvc.perform(post("/api/v1/shifts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildValidRequest())))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // GET /api/v1/shifts RBAC
    // =========================================================================

    @Test
    @DisplayName("GET /api/v1/shifts returns 200 for ROLE_EMPLOYEE")
    @WithMockUser(roles = "EMPLOYEE")
    void getAllShifts_allowedForEmployee() throws Exception {
        when(shiftService.getAllShifts()).thenReturn(List.of(buildResponse(UUID.randomUUID())));

        mockMvc.perform(get("/api/v1/shifts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].shiftName").value("General Shift"));
    }

    @Test
    @DisplayName("GET /api/v1/shifts returns 200 for ROLE_HR")
    @WithMockUser(roles = "HR")
    void getAllShifts_allowedForHr() throws Exception {
        when(shiftService.getAllShifts()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/shifts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("GET /api/v1/shifts returns 403 for ROLE_FINANCE")
    @WithMockUser(roles = "FINANCE")
    void getAllShifts_forbiddenForFinance() throws Exception {
        mockMvc.perform(get("/api/v1/shifts"))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // GET /api/v1/shifts/{id} RBAC
    // =========================================================================

    @Test
    @DisplayName("GET /api/v1/shifts/{id} returns 200 for ROLE_EMPLOYEE")
    @WithMockUser(roles = "EMPLOYEE")
    void getShiftById_allowedForEmployee() throws Exception {
        UUID id = UUID.randomUUID();
        when(shiftService.getShiftById(id)).thenReturn(buildResponse(id));

        mockMvc.perform(get("/api/v1/shifts/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(id.toString()));
    }

    @Test
    @DisplayName("GET /api/v1/shifts/{id} returns 403 for ROLE_CA")
    @WithMockUser(roles = "CA")
    void getShiftById_forbiddenForCa() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(get("/api/v1/shifts/" + id))
                .andExpect(status().isForbidden());
    }
}

package com.ourhr.payroll;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ourhr.payroll.dto.CreateLopReversalRequest;
import com.ourhr.payroll.dto.LopReversalResponse;
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

@WebMvcTest(LopReversalController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class LopReversalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LopReversalService lopReversalService;

    @MockBean
    private JwtService jwtService;

    @Test
    @DisplayName("HR should successfully create LOP reversal")
    @WithMockUser(roles = "HR")
    void hrShouldCreateLopReversal() throws Exception {
        UUID empId = UUID.randomUUID();
        CreateLopReversalRequest request = CreateLopReversalRequest.builder()
                .employeeId(empId)
                .originalPayrollMonth(4)
                .originalPayrollYear(2026)
                .reversedDays(new BigDecimal("1.5"))
                .reason("Punch regularization")
                .build();

        LopReversalResponse response = LopReversalResponse.builder()
                .id(UUID.randomUUID())
                .employeeId(empId)
                .reversedDays(new BigDecimal("1.5"))
                .reversalAmount(new BigDecimal("3000.00"))
                .status("PENDING")
                .build();

        when(lopReversalService.createLopReversal(any(CreateLopReversalRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/payroll/lop-reversals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    @DisplayName("FINANCE should successfully approve LOP reversal")
    @WithMockUser(roles = "FINANCE")
    void financeShouldApproveLopReversal() throws Exception {
        UUID reversalId = UUID.randomUUID();
        LopReversalResponse response = LopReversalResponse.builder()
                .id(reversalId)
                .status("APPROVED")
                .reversalAmount(new BigDecimal("3000.00"))
                .build();

        when(lopReversalService.approveLopReversal(eq(reversalId))).thenReturn(response);

        mockMvc.perform(post("/api/v1/payroll/lop-reversals/" + reversalId + "/approve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("APPROVED"));
    }

    @Test
    @DisplayName("HR should be forbidden from approving LOP reversal")
    @WithMockUser(roles = "HR")
    void hrForbiddenFromApproving() throws Exception {
        UUID reversalId = UUID.randomUUID();
        mockMvc.perform(post("/api/v1/payroll/lop-reversals/" + reversalId + "/approve"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("FOUNDER should retrieve list of pending reversals")
    @WithMockUser(roles = "FOUNDER")
    void founderShouldListPending() throws Exception {
        LopReversalResponse response = LopReversalResponse.builder()
                .id(UUID.randomUUID())
                .status("PENDING")
                .reversalAmount(new BigDecimal("2500.00"))
                .build();

        when(lopReversalService.getPendingReversals()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/payroll/lop-reversals/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].status").value("PENDING"));
    }
}

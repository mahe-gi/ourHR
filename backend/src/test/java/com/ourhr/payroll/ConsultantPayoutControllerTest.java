package com.ourhr.payroll;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ourhr.payroll.dto.ConsultantPayoutRequest;
import com.ourhr.payroll.dto.ConsultantPayoutResponse;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ConsultantPayoutController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class ConsultantPayoutControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ConsultantPayoutService consultantPayoutService;

    @MockBean
    private JwtService jwtService;

    @Test
    @DisplayName("FOUNDER should successfully execute Section 194J payout")
    @WithMockUser(roles = "FOUNDER")
    void founderShouldExecute194JPayout() throws Exception {
        UUID empId = UUID.randomUUID();
        ConsultantPayoutRequest request = ConsultantPayoutRequest.builder()
                .consultantEmployeeId(empId)
                .grossAmount(new BigDecimal("100000.00"))
                .tdsSection("194J")
                .tdsRate(new BigDecimal("10.0"))
                .description("Consulting retainer")
                .build();

        ConsultantPayoutResponse response = ConsultantPayoutResponse.builder()
                .id(UUID.randomUUID())
                .consultantEmployeeId(empId)
                .consultantName("Rohan Roy")
                .grossAmount(new BigDecimal("100000.00"))
                .tdsSection("194J")
                .tdsRate(new BigDecimal("10.0"))
                .tdsAmount(new BigDecimal("10000.00"))
                .netPayable(new BigDecimal("90000.00"))
                .status("PROCESSED")
                .build();

        when(consultantPayoutService.processConsultantPayout(any(ConsultantPayoutRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/payroll/consultants/payout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.netPayable").value(90000.00))
                .andExpect(jsonPath("$.data.tdsAmount").value(10000.00));
    }

    @Test
    @DisplayName("HR should be forbidden from processing consultant payouts")
    @WithMockUser(roles = "HR")
    void hrForbiddenFromConsultantPayout() throws Exception {
        UUID empId = UUID.randomUUID();
        ConsultantPayoutRequest request = ConsultantPayoutRequest.builder()
                .consultantEmployeeId(empId)
                .grossAmount(new BigDecimal("50000.00"))
                .tdsSection("194J")
                .tdsRate(new BigDecimal("10.0"))
                .build();

        mockMvc.perform(post("/api/v1/payroll/consultants/payout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}

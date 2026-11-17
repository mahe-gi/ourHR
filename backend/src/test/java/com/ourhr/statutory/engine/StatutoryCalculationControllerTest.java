package com.ourhr.statutory.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ourhr.common.ApiResponse;
import com.ourhr.security.JwtAuthenticationFilter;
import com.ourhr.security.JwtService;
import com.ourhr.security.SecurityConfig;
import com.ourhr.statutory.engine.dto.EpfCalculationResult;
import com.ourhr.statutory.engine.dto.EsicCalculationResult;
import com.ourhr.statutory.engine.dto.LwfCalculationResult;
import com.ourhr.statutory.engine.dto.PtCalculationResult;
import com.ourhr.statutory.engine.dto.StatutoryCalculationRequest;
import com.ourhr.statutory.engine.dto.StatutoryCalculationResult;
import com.ourhr.statutory.engine.dto.TdsCalculationResult;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StatutoryCalculationController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class StatutoryCalculationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private IndianStatutoryEngineService statutoryEngineService;

    @MockBean
    private JwtService jwtService;

    private StatutoryCalculationRequest buildSampleRequest() {
        return StatutoryCalculationRequest.builder()
                .employeeId(UUID.randomUUID())
                .companyId(UUID.randomUUID())
                .branchStateCode("MH")
                .gender("MALE")
                .earnedGross(new BigDecimal("30000.00"))
                .basicPay(new BigDecimal("15000.00"))
                .daPay(BigDecimal.ZERO)
                .payrollMonth(4)
                .payrollYear(2026)
                .financialYear("2026-2027")
                .taxRegime("NEW")
                .build();
    }

    private StatutoryCalculationResult buildSampleResult() {
        return StatutoryCalculationResult.builder()
                .epf(EpfCalculationResult.builder()
                        .wageBase(new BigDecimal("15000.00"))
                        .employeeContribution(new BigDecimal("1800.00"))
                        .employerEpsContribution(new BigDecimal("1250.00"))
                        .employerEpfContribution(new BigDecimal("550.00"))
                        .edliContribution(new BigDecimal("75.00"))
                        .adminCharges(new BigDecimal("75.00"))
                        .totalEmployerLiability(new BigDecimal("1950.00"))
                        .build())
                .esic(EsicCalculationResult.builder()
                        .eligible(false)
                        .wageBase(BigDecimal.ZERO)
                        .employeeContribution(BigDecimal.ZERO)
                        .employerContribution(BigDecimal.ZERO)
                        .totalLiability(BigDecimal.ZERO)
                        .note("Earned gross exceeds ESIC wage ceiling of 21000.00")
                        .build())
                .pt(PtCalculationResult.builder()
                        .stateCode("MH")
                        .salaryAmount(new BigDecimal("30000.00"))
                        .taxAmount(new BigDecimal("200.00"))
                        .isSpecialMonth(false)
                        .build())
                .lwf(LwfCalculationResult.builder()
                        .stateCode("MH")
                        .applicable(false)
                        .employeeContribution(BigDecimal.ZERO)
                        .employerContribution(BigDecimal.ZERO)
                        .totalContribution(BigDecimal.ZERO)
                        .build())
                .tds(TdsCalculationResult.builder()
                        .financialYear("2026-2027")
                        .regime("NEW")
                        .projectedAnnualGross(new BigDecimal("360000.00"))
                        .standardDeduction(new BigDecimal("75000.00"))
                        .taxableIncome(new BigDecimal("285000.00"))
                        .taxBeforeCess(BigDecimal.ZERO)
                        .section87aRebate(BigDecimal.ZERO)
                        .taxAfterRebate(BigDecimal.ZERO)
                        .educationCess(BigDecimal.ZERO)
                        .totalAnnualTax(BigDecimal.ZERO)
                        .taxPaidSoFar(BigDecimal.ZERO)
                        .remainingMonths(12)
                        .monthlyTdsDeduction(BigDecimal.ZERO)
                        .build())
                .totalEmployeeDeductions(new BigDecimal("2000.00"))
                .totalEmployerContributions(new BigDecimal("1950.00"))
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/statutory/calculate allows ROLE_FOUNDER")
    @WithMockUser(roles = "FOUNDER")
    void shouldAllowFounderRole() throws Exception {
        StatutoryCalculationRequest request = buildSampleRequest();
        when(statutoryEngineService.calculate(any())).thenReturn(buildSampleResult());

        mockMvc.perform(post("/api/v1/statutory/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.epf.employeeContribution").value(1800.00))
                .andExpect(jsonPath("$.data.totalEmployeeDeductions").value(2000.00));

        verify(statutoryEngineService).calculate(any());
    }

    @Test
    @DisplayName("POST /api/v1/statutory/calculate allows ROLE_HR")
    @WithMockUser(roles = "HR")
    void shouldAllowHrRole() throws Exception {
        StatutoryCalculationRequest request = buildSampleRequest();
        when(statutoryEngineService.calculate(any())).thenReturn(buildSampleResult());

        mockMvc.perform(post("/api/v1/statutory/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(statutoryEngineService).calculate(any());
    }

    @Test
    @DisplayName("POST /api/v1/statutory/calculate allows ROLE_FINANCE")
    @WithMockUser(roles = "FINANCE")
    void shouldAllowFinanceRole() throws Exception {
        StatutoryCalculationRequest request = buildSampleRequest();
        when(statutoryEngineService.calculate(any())).thenReturn(buildSampleResult());

        mockMvc.perform(post("/api/v1/statutory/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(statutoryEngineService).calculate(any());
    }

    @Test
    @DisplayName("POST /api/v1/statutory/calculate allows ROLE_CA")
    @WithMockUser(roles = "CA")
    void shouldAllowCaRole() throws Exception {
        StatutoryCalculationRequest request = buildSampleRequest();
        when(statutoryEngineService.calculate(any())).thenReturn(buildSampleResult());

        mockMvc.perform(post("/api/v1/statutory/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(statutoryEngineService).calculate(any());
    }

    @Test
    @DisplayName("POST /api/v1/statutory/calculate returns 403 Forbidden for ROLE_EMPLOYEE")
    @WithMockUser(roles = "EMPLOYEE")
    void shouldForbidEmployeeRole() throws Exception {
        StatutoryCalculationRequest request = buildSampleRequest();

        mockMvc.perform(post("/api/v1/statutory/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/statutory/calculate returns 403 Forbidden for Anonymous")
    void shouldForbidAnonymous() throws Exception {
        StatutoryCalculationRequest request = buildSampleRequest();

        mockMvc.perform(post("/api/v1/statutory/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}

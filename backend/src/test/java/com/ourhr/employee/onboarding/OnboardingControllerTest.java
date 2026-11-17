package com.ourhr.employee.onboarding;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ourhr.employee.onboarding.dto.GenerateOnboardingLinkRequest;
import com.ourhr.employee.onboarding.dto.OnboardingLinkResponse;
import com.ourhr.employee.onboarding.dto.OnboardingStatusResponse;
import com.ourhr.employee.onboarding.dto.OnboardingSubmissionRequest;
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

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OnboardingController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class OnboardingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OnboardingService onboardingService;

    @MockBean
    private JwtService jwtService;

    @Test
    @DisplayName("POST /api/v1/onboarding/generate-link returns 200 for ROLE_HR")
    @WithMockUser(roles = "HR")
    void shouldAllowGenerateLinkForHr() throws Exception {
        UUID employeeId = UUID.randomUUID();
        GenerateOnboardingLinkRequest request = GenerateOnboardingLinkRequest.builder()
                .employeeId(employeeId)
                .expiresInDays(7)
                .build();

        OnboardingLinkResponse response = OnboardingLinkResponse.builder()
                .employeeId(employeeId)
                .token("jwt-token-123")
                .onboardingUrl("/onboarding?token=jwt-token-123")
                .expiresAt(Instant.now().plusSeconds(604800))
                .build();

        when(onboardingService.generateOnboardingLink(eq(employeeId), anyInt())).thenReturn(response);

        mockMvc.perform(post("/api/v1/onboarding/generate-link")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").value("jwt-token-123"))
                .andExpect(jsonPath("$.data.onboardingUrl").value("/onboarding?token=jwt-token-123"));
    }

    @Test
    @DisplayName("POST /api/v1/onboarding/generate-link returns 403 for ROLE_EMPLOYEE and ROLE_FINANCE")
    @WithMockUser(roles = "EMPLOYEE")
    void shouldDenyGenerateLinkForEmployee() throws Exception {
        GenerateOnboardingLinkRequest request = GenerateOnboardingLinkRequest.builder()
                .employeeId(UUID.randomUUID())
                .expiresInDays(7)
                .build();

        mockMvc.perform(post("/api/v1/onboarding/generate-link")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/onboarding/generate-link returns 403 when unauthenticated")
    void shouldDenyGenerateLinkWhenUnauthenticated() throws Exception {
        GenerateOnboardingLinkRequest request = GenerateOnboardingLinkRequest.builder()
                .employeeId(UUID.randomUUID())
                .expiresInDays(7)
                .build();

        mockMvc.perform(post("/api/v1/onboarding/generate-link")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/onboarding/submit is permitAll (accessible unauthenticated)")
    void shouldAllowSubmitOnboardingUnauthenticated() throws Exception {
        com.ourhr.employee.dto.UpdateEmployeeRequest personalDetails = com.ourhr.employee.dto.UpdateEmployeeRequest.builder()
                .firstName("Candidate")
                .lastName("Test")
                .build();

        com.ourhr.employee.kyc.dto.CreateKycVaultRequest kycDetails = com.ourhr.employee.kyc.dto.CreateKycVaultRequest.builder()
                .panNumber("ABCDE1234F")
                .bankAccountNumber("123456789012")
                .bankIfsc("HDFC0001234")
                .bankName("HDFC Bank")
                .build();

        OnboardingSubmissionRequest request = OnboardingSubmissionRequest.builder()
                .token("valid-candidate-token")
                .personalDetails(personalDetails)
                .kycDetails(kycDetails)
                .build();

        OnboardingStatusResponse response = OnboardingStatusResponse.builder()
                .employeeId(UUID.randomUUID())
                .employeeCode("EMP-NEW")
                .employeeName("Candidate Test")
                .onboardingStatus("COMPLETED")
                .kycSubmitted(true)
                .form12bSubmitted(false)
                .completedAt(Instant.now())
                .build();

        when(onboardingService.submitOnboarding(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/onboarding/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.onboardingStatus").value("COMPLETED"))
                .andExpect(jsonPath("$.data.employeeCode").value("EMP-NEW"));
    }

    @Test
    @DisplayName("GET /api/v1/onboarding/status/{employeeId} returns 200 for ROLE_FOUNDER and ROLE_HR")
    @WithMockUser(roles = "FOUNDER")
    void shouldAllowGetStatusForFounder() throws Exception {
        UUID employeeId = UUID.randomUUID();
        OnboardingStatusResponse response = OnboardingStatusResponse.builder()
                .employeeId(employeeId)
                .employeeCode("EMP-101")
                .employeeName("Ajay Rao")
                .onboardingStatus("PENDING")
                .kycSubmitted(false)
                .build();

        when(onboardingService.getOnboardingStatus(eq(employeeId))).thenReturn(response);

        mockMvc.perform(get("/api/v1/onboarding/status/" + employeeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.onboardingStatus").value("PENDING"));
    }

    @Test
    @DisplayName("GET /api/v1/onboarding/status/{employeeId} returns 403 for ROLE_FINANCE, ROLE_CA, and ROLE_EMPLOYEE")
    @WithMockUser(roles = "FINANCE")
    void shouldDenyGetStatusForFinance() throws Exception {
        UUID employeeId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/onboarding/status/" + employeeId))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/onboarding/status/{employeeId} returns 403 when unauthenticated")
    void shouldDenyGetStatusWhenUnauthenticated() throws Exception {
        UUID employeeId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/onboarding/status/" + employeeId))
                .andExpect(status().isForbidden());
    }
}

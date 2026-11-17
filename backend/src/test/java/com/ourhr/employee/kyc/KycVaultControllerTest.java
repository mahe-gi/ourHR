package com.ourhr.employee.kyc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ourhr.common.ResourceNotFoundException;
import com.ourhr.employee.kyc.dto.CreateKycVaultRequest;
import com.ourhr.employee.kyc.dto.KycVaultResponse;
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

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(KycVaultController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class KycVaultControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private KycVaultService kycVaultService;

    @MockBean
    private JwtService jwtService;

    private CreateKycVaultRequest buildValidRequest(UUID employeeId) {
        return CreateKycVaultRequest.builder()
                .employeeId(employeeId)
                .panNumber("ABCDE1234F")
                .aadhaarNumber("999988887777")
                .uan("100900800700")
                .esicIpNumber("12345678901234567")
                .bankAccountNumber("123456789012")
                .bankIfsc("HDFC0001234")
                .bankName("HDFC Bank")
                .taxRegimePreference("NEW")
                .build();
    }

    private KycVaultResponse buildSampleResponse(UUID employeeId) {
        return KycVaultResponse.builder()
                .id(UUID.randomUUID())
                .employeeId(employeeId)
                .panNumber("ABCDE1234F")
                .maskedAadhaarToken("[Aadhaar Redacted]")
                .bankAccountNumberMasked("******9012")
                .bankIfsc("HDFC0001234")
                .bankName("HDFC Bank")
                .taxRegimePreference("NEW")
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/employees/{employeeId}/kyc returns 200 for ROLE_HR")
    @WithMockUser(roles = "HR")
    void shouldAllowSaveKycForHr() throws Exception {
        UUID employeeId = UUID.randomUUID();
        CreateKycVaultRequest request = buildValidRequest(employeeId);
        when(kycVaultService.saveKycVault(eq(employeeId), any())).thenReturn(buildSampleResponse(employeeId));

        mockMvc.perform(post("/api/v1/employees/" + employeeId + "/kyc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.maskedAadhaarToken").value("[Aadhaar Redacted]"))
                .andExpect(jsonPath("$.data.bankAccountNumberMasked").value("******9012"));
    }

    @Test
    @DisplayName("POST /api/v1/employees/{employeeId}/kyc returns 200 for ROLE_EMPLOYEE (self-service)")
    @WithMockUser(roles = "EMPLOYEE")
    void shouldAllowSaveKycForEmployee() throws Exception {
        UUID employeeId = UUID.randomUUID();
        CreateKycVaultRequest request = buildValidRequest(employeeId);
        when(kycVaultService.saveKycVault(eq(employeeId), any())).thenReturn(buildSampleResponse(employeeId));

        mockMvc.perform(post("/api/v1/employees/" + employeeId + "/kyc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("POST /api/v1/employees/{employeeId}/kyc returns 403 for ROLE_FINANCE and ROLE_CA")
    @WithMockUser(roles = "FINANCE")
    void shouldDenySaveKycForFinance() throws Exception {
        UUID employeeId = UUID.randomUUID();
        CreateKycVaultRequest request = buildValidRequest(employeeId);

        mockMvc.perform(post("/api/v1/employees/" + employeeId + "/kyc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/employees/{employeeId}/kyc returns 403 when unauthenticated")
    void shouldDenySaveKycWhenUnauthenticated() throws Exception {
        UUID employeeId = UUID.randomUUID();
        CreateKycVaultRequest request = buildValidRequest(employeeId);

        mockMvc.perform(post("/api/v1/employees/" + employeeId + "/kyc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/employees/{employeeId}/kyc returns 200 with masked sensitive data for ROLE_CA")
    @WithMockUser(roles = "CA")
    void shouldAllowGetKycForCa() throws Exception {
        UUID employeeId = UUID.randomUUID();
        when(kycVaultService.getKycVault(eq(employeeId))).thenReturn(buildSampleResponse(employeeId));

        mockMvc.perform(get("/api/v1/employees/" + employeeId + "/kyc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.maskedAadhaarToken").value("[Aadhaar Redacted]"))
                .andExpect(jsonPath("$.data.bankAccountNumberMasked").value("******9012"));
    }

    @Test
    @DisplayName("GET /api/v1/employees/{employeeId}/kyc returns 200 for ROLE_FINANCE")
    @WithMockUser(roles = "FINANCE")
    void shouldAllowGetKycForFinance() throws Exception {
        UUID employeeId = UUID.randomUUID();
        when(kycVaultService.getKycVault(eq(employeeId))).thenReturn(buildSampleResponse(employeeId));

        mockMvc.perform(get("/api/v1/employees/" + employeeId + "/kyc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("GET /api/v1/employees/{employeeId}/kyc returns 403 when unauthenticated")
    void shouldDenyGetKycWhenUnauthenticated() throws Exception {
        UUID employeeId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/employees/" + employeeId + "/kyc"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/employees/{employeeId}/kyc returns 404 when KYC vault not found")
    @WithMockUser(roles = "HR")
    void shouldReturn404WhenKycNotFound() throws Exception {
        UUID employeeId = UUID.randomUUID();
        when(kycVaultService.getKycVault(eq(employeeId)))
                .thenThrow(new ResourceNotFoundException("KYC vault not found"));

        mockMvc.perform(get("/api/v1/employees/" + employeeId + "/kyc"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
    }
}

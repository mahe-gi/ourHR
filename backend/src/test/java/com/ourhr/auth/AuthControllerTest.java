package com.ourhr.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ourhr.auth.dto.AuthResponse;
import com.ourhr.auth.dto.LoginRequest;
import com.ourhr.auth.dto.RegisterCompanyRequest;
import com.ourhr.auth.dto.RegisterCompanyResponse;
import com.ourhr.auth.dto.UserProfileResponse;
import com.ourhr.security.JwtAuthenticationFilter;
import com.ourhr.security.JwtService;
import com.ourhr.security.Role;
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

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtService jwtService;

    @Test
    @DisplayName("POST /api/v1/auth/register-company should return 201 Created for valid input")
    void shouldRegisterCompanySuccessfully() throws Exception {
        RegisterCompanyRequest request = RegisterCompanyRequest.builder()
                .legalName("Acme Technologies Pvt Ltd")
                .panNumber("ABCDE1234F")
                .tanNumber("BLRA12345E")
                .signatoryName("Rahul Sharma")
                .signatoryDesignation("Managing Director")
                .stateCode("KA")
                .city("Bangalore")
                .addressLine("100 Feet Road, Indiranagar")
                .pincode("560038")
                .adminEmail("founder@acme.com")
                .adminPassword("SecurePass123")
                .adminFullName("Rahul Sharma")
                .build();

        UUID companyId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        RegisterCompanyResponse response = RegisterCompanyResponse.builder()
                .companyId(companyId)
                .legalName("Acme Technologies Pvt Ltd")
                .adminUserId(userId)
                .adminEmail("founder@acme.com")
                .adminFullName("Rahul Sharma")
                .message("Company and administrator successfully registered")
                .build();

        when(authService.registerCompany(any(RegisterCompanyRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/register-company")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.companyId").value(companyId.toString()))
                .andExpect(jsonPath("$.data.adminEmail").value("founder@acme.com"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register-company should return 400 Bad Request for invalid PAN format")
    void shouldRejectInvalidPanFormat() throws Exception {
        RegisterCompanyRequest request = RegisterCompanyRequest.builder()
                .legalName("Acme Technologies Pvt Ltd")
                .panNumber("INVALID_PAN") // Bad format
                .tanNumber("BLRA12345E")
                .signatoryName("Rahul Sharma")
                .signatoryDesignation("Managing Director")
                .stateCode("KA")
                .city("Bangalore")
                .addressLine("100 Feet Road, Indiranagar")
                .pincode("560038")
                .adminEmail("founder@acme.com")
                .adminPassword("SecurePass123")
                .adminFullName("Rahul Sharma")
                .build();

        mockMvc.perform(post("/api/v1/auth/register-company")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.error.details[0].field").value("panNumber"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login should return 200 OK and token for valid credentials")
    void shouldLoginSuccessfully() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("founder@acme.com")
                .password("SecurePass123")
                .build();

        UUID companyId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        AuthResponse response = AuthResponse.builder()
                .accessToken("mock.access.token")
                .tokenType("Bearer")
                .expiresInMs(86400000L)
                .userId(userId)
                .companyId(companyId)
                .companyName("Acme Technologies Pvt Ltd")
                .email("founder@acme.com")
                .fullName("Rahul Sharma")
                .roles(List.of(Role.ROLE_FOUNDER))
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("mock.access.token"))
                .andExpect(jsonPath("$.data.email").value("founder@acme.com"));
    }

    @Test
    @DisplayName("GET /api/v1/auth/me should return 403 Forbidden when unauthenticated")
    void shouldRejectUnauthenticatedAccessToMe() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "founder@acme.com", roles = {"FOUNDER"})
    @DisplayName("GET /api/v1/auth/me should return 200 OK and profile when authenticated")
    void shouldReturnProfileWhenAuthenticated() throws Exception {
        UUID companyId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        UserProfileResponse response = UserProfileResponse.builder()
                .userId(userId)
                .companyId(companyId)
                .companyName("Acme Technologies Pvt Ltd")
                .email("founder@acme.com")
                .fullName("Rahul Sharma")
                .roles(List.of(Role.ROLE_FOUNDER))
                .isActive(true)
                .build();

        when(authService.getCurrentUser("founder@acme.com")).thenReturn(response);

        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("founder@acme.com"))
                .andExpect(jsonPath("$.data.companyName").value("Acme Technologies Pvt Ltd"));
    }
}

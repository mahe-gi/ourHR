package com.ourhr.company;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ourhr.common.ResourceNotFoundException;
import com.ourhr.company.dto.CompanyResponse;
import com.ourhr.company.dto.UpdateCompanyRequest;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CompanyController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class CompanyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CompanyService companyService;

    @MockBean
    private JwtService jwtService;

    @Test
    @DisplayName("GET /api/v1/companies/current should return 200 for ROLE_FOUNDER")
    @WithMockUser(roles = "FOUNDER")
    void shouldReturnCompanyForFounder() throws Exception {
        UUID companyId = UUID.randomUUID();
        CompanyResponse response = CompanyResponse.builder()
                .id(companyId)
                .legalName("Acme Technologies Pvt Ltd")
                .panNumber("ABCDE1234F")
                .tanNumber("BLRA12345E")
                .signatoryName("Rahul Sharma")
                .signatoryDesignation("Director")
                .build();

        when(companyService.getCurrentCompany()).thenReturn(response);

        mockMvc.perform(get("/api/v1/companies/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.legalName").value("Acme Technologies Pvt Ltd"))
                .andExpect(jsonPath("$.data.panNumber").value("ABCDE1234F"));
    }

    @Test
    @DisplayName("GET /api/v1/companies/current should return 200 for ROLE_HR")
    @WithMockUser(roles = "HR")
    void shouldReturnCompanyForHr() throws Exception {
        CompanyResponse response = CompanyResponse.builder()
                .id(UUID.randomUUID())
                .legalName("Acme Technologies Pvt Ltd")
                .build();

        when(companyService.getCurrentCompany()).thenReturn(response);

        mockMvc.perform(get("/api/v1/companies/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("GET /api/v1/companies/current should return 200 for ROLE_CA")
    @WithMockUser(roles = "CA")
    void shouldReturnCompanyForCa() throws Exception {
        CompanyResponse response = CompanyResponse.builder()
                .id(UUID.randomUUID())
                .legalName("Acme Technologies Pvt Ltd")
                .build();

        when(companyService.getCurrentCompany()).thenReturn(response);

        mockMvc.perform(get("/api/v1/companies/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("GET /api/v1/companies/current should return 403 for ROLE_EMPLOYEE")
    @WithMockUser(roles = "EMPLOYEE")
    void shouldDenyCompanyForEmployee() throws Exception {
        mockMvc.perform(get("/api/v1/companies/current"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/companies/current should return 403/401 when unauthenticated")
    void shouldDenyCompanyWhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/companies/current"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /api/v1/companies/current should return 200 for ROLE_FOUNDER")
    @WithMockUser(roles = "FOUNDER")
    void shouldUpdateCompanyForFounder() throws Exception {
        UpdateCompanyRequest request = UpdateCompanyRequest.builder()
                .tradeName("Acme Innovations")
                .signatoryName("Amit Shah")
                .signatoryDesignation("CEO")
                .build();

        CompanyResponse response = CompanyResponse.builder()
                .id(UUID.randomUUID())
                .legalName("Acme Technologies Pvt Ltd")
                .tradeName("Acme Innovations")
                .signatoryName("Amit Shah")
                .signatoryDesignation("CEO")
                .build();

        when(companyService.updateCurrentCompany(any(UpdateCompanyRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/companies/current")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.tradeName").value("Acme Innovations"))
                .andExpect(jsonPath("$.data.signatoryName").value("Amit Shah"));
    }

    @Test
    @DisplayName("PUT /api/v1/companies/current should return 403 for ROLE_FINANCE")
    @WithMockUser(roles = "FINANCE")
    void shouldDenyUpdateCompanyForFinance() throws Exception {
        UpdateCompanyRequest request = UpdateCompanyRequest.builder()
                .tradeName("Acme Innovations")
                .build();

        mockMvc.perform(put("/api/v1/companies/current")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/companies/current should return 404 when company not found")
    @WithMockUser(roles = "FOUNDER")
    void shouldReturn404WhenNotFound() throws Exception {
        when(companyService.getCurrentCompany()).thenThrow(new ResourceNotFoundException("Company not found"));

        mockMvc.perform(get("/api/v1/companies/current"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
    }
}

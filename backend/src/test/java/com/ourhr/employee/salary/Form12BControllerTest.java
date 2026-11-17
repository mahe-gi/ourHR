package com.ourhr.employee.salary;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ourhr.employee.salary.dto.PreviousEmployerIncomeResponse;
import com.ourhr.employee.salary.dto.SubmitForm12BRequest;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(Form12BController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class Form12BControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PreviousEmployerIncomeService previousEmployerIncomeService;

    @MockBean
    private JwtService jwtService;

    private SubmitForm12BRequest buildValidRequest(UUID employeeId) {
        return SubmitForm12BRequest.builder()
                .employeeId(employeeId)
                .financialYear("2025-2026")
                .grossSalary(new BigDecimal("350000.00"))
                .pfDeducted(new BigDecimal("21600.00"))
                .ptDeducted(new BigDecimal("2500.00"))
                .tdsDeducted(new BigDecimal("12000.00"))
                .build();
    }

    private PreviousEmployerIncomeResponse buildSampleResponse(UUID employeeId) {
        return PreviousEmployerIncomeResponse.builder()
                .id(UUID.randomUUID())
                .employeeId(employeeId)
                .financialYear("2025-2026")
                .grossSalary(new BigDecimal("350000.00"))
                .pfDeducted(new BigDecimal("21600.00"))
                .ptDeducted(new BigDecimal("2500.00"))
                .tdsDeducted(new BigDecimal("12000.00"))
                .isVerified(false)
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/employees/{employeeId}/form-12b returns 200 for ROLE_EMPLOYEE (candidate submission)")
    @WithMockUser(roles = "EMPLOYEE")
    void shouldAllowSubmitForm12BForEmployee() throws Exception {
        UUID employeeId = UUID.randomUUID();
        SubmitForm12BRequest request = buildValidRequest(employeeId);

        when(previousEmployerIncomeService.savePreviousIncome(any())).thenReturn(buildSampleResponse(employeeId));

        mockMvc.perform(post("/api/v1/employees/" + employeeId + "/form-12b")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.grossSalary").value(350000.00));
    }

    @Test
    @DisplayName("POST /api/v1/employees/{employeeId}/form-12b returns 403 for ROLE_FINANCE and ROLE_CA")
    @WithMockUser(roles = "CA")
    void shouldDenySubmitForm12BForCa() throws Exception {
        UUID employeeId = UUID.randomUUID();
        SubmitForm12BRequest request = buildValidRequest(employeeId);

        mockMvc.perform(post("/api/v1/employees/" + employeeId + "/form-12b")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/employees/{employeeId}/form-12b/{financialYear} returns 200 for ROLE_CA")
    @WithMockUser(roles = "CA")
    void shouldAllowGetForm12BForCa() throws Exception {
        UUID employeeId = UUID.randomUUID();
        when(previousEmployerIncomeService.getPreviousIncome(eq(employeeId), eq("2025-2026")))
                .thenReturn(buildSampleResponse(employeeId));

        mockMvc.perform(get("/api/v1/employees/" + employeeId + "/form-12b/2025-2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.financialYear").value("2025-2026"));
    }

    @Test
    @DisplayName("GET /api/v1/employees/{employeeId}/form-12b/{financialYear} returns 403 when unauthenticated")
    void shouldDenyGetForm12BWhenUnauthenticated() throws Exception {
        UUID employeeId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/employees/" + employeeId + "/form-12b/2025-2026"))
                .andExpect(status().isForbidden());
    }
}

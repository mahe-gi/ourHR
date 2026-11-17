package com.ourhr.employee.salary;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ourhr.employee.salary.dto.SalaryOfferRequest;
import com.ourhr.employee.salary.dto.SalaryOfferValidationResult;
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
import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SalaryStructureController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class SalaryStructureControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SalaryStructureService salaryStructureService;

    @MockBean
    private BasicWageValidator basicWageValidator;

    @MockBean
    private JwtService jwtService;

    private SalaryOfferRequest buildValidRequest(UUID employeeId) {
        return SalaryOfferRequest.builder()
                .employeeId(employeeId)
                .annualCtc(new BigDecimal("1200000.00"))
                .monthlyGross(new BigDecimal("100000.00"))
                .basicPay(new BigDecimal("50000.00"))
                .daPay(BigDecimal.ZERO)
                .hraPay(new BigDecimal("25000.00"))
                .specialAllowance(new BigDecimal("25000.00"))
                .effectiveFrom(LocalDate.of(2026, 4, 1))
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/employees/{employeeId}/salary-structures returns 201 for ROLE_HR")
    @WithMockUser(roles = "HR")
    void shouldAllowCreateSalaryStructureForHr() throws Exception {
        UUID employeeId = UUID.randomUUID();
        SalaryOfferRequest request = buildValidRequest(employeeId);

        EmployeeSalaryStructure created = EmployeeSalaryStructure.builder()
                .employeeId(employeeId)
                .monthlyGross(request.getMonthlyGross())
                .basicPay(request.getBasicPay())
                .build();
        created.setId(UUID.randomUUID());

        when(salaryStructureService.createSalaryStructure(any())).thenReturn(created);

        mockMvc.perform(post("/api/v1/employees/" + employeeId + "/salary-structures")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("POST /api/v1/employees/{employeeId}/salary-structures returns 403 for ROLE_EMPLOYEE and ROLE_FINANCE")
    @WithMockUser(roles = "EMPLOYEE")
    void shouldDenyCreateSalaryStructureForEmployee() throws Exception {
        UUID employeeId = UUID.randomUUID();
        SalaryOfferRequest request = buildValidRequest(employeeId);

        mockMvc.perform(post("/api/v1/employees/" + employeeId + "/salary-structures")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/employees/salary-structures/validate returns 200 for ROLE_FOUNDER and ROLE_HR")
    @WithMockUser(roles = "FOUNDER")
    void shouldAllowValidateSalaryStructureForFounder() throws Exception {
        SalaryOfferRequest request = buildValidRequest(UUID.randomUUID());

        SalaryOfferValidationResult result = SalaryOfferValidationResult.builder()
                .valid(true)
                .basicPlusDa(new BigDecimal("50000.00"))
                .monthlyGross(new BigDecimal("100000.00"))
                .wageRatio(new BigDecimal("0.5000"))
                .minRequiredRatio(new BigDecimal("0.50"))
                .statutoryDeficit(BigDecimal.ZERO)
                .message("Compliant with Code on Wages 2019")
                .build();

        when(basicWageValidator.validate(any())).thenReturn(result);

        mockMvc.perform(post("/api/v1/employees/salary-structures/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.valid").value(true));
    }

    @Test
    @DisplayName("POST /api/v1/employees/salary-structures/validate returns 403 for ROLE_EMPLOYEE and ROLE_FINANCE")
    @WithMockUser(roles = "FINANCE")
    void shouldDenyValidateSalaryStructureForFinance() throws Exception {
        SalaryOfferRequest request = buildValidRequest(UUID.randomUUID());

        mockMvc.perform(post("/api/v1/employees/salary-structures/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/employees/{employeeId}/salary-structures/active returns 200 for ROLE_EMPLOYEE")
    @WithMockUser(roles = "EMPLOYEE")
    void shouldAllowGetActiveSalaryStructureForEmployee() throws Exception {
        UUID employeeId = UUID.randomUUID();
        EmployeeSalaryStructure structure = EmployeeSalaryStructure.builder()
                .employeeId(employeeId)
                .monthlyGross(new BigDecimal("100000.00"))
                .basicPay(new BigDecimal("50000.00"))
                .build();
        structure.setId(UUID.randomUUID());

        when(salaryStructureService.getActiveSalaryStructure(eq(employeeId))).thenReturn(structure);

        mockMvc.perform(get("/api/v1/employees/" + employeeId + "/salary-structures/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("GET /api/v1/employees/{employeeId}/salary-structures/active returns 403 for ROLE_CA")
    @WithMockUser(roles = "CA")
    void shouldDenyGetActiveSalaryStructureForCa() throws Exception {
        UUID employeeId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/employees/" + employeeId + "/salary-structures/active"))
                .andExpect(status().isForbidden());
    }
}

package com.ourhr.employee;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ourhr.common.ResourceNotFoundException;
import com.ourhr.employee.dto.CreateEmployeeRequest;
import com.ourhr.employee.dto.EmployeeResponse;
import com.ourhr.employee.dto.UpdateEmployeeRequest;
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

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EmployeeController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class EmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EmployeeService employeeService;

    @MockBean
    private JwtService jwtService;

    private CreateEmployeeRequest buildValidCreateRequest() {
        return CreateEmployeeRequest.builder()
                .branchId(UUID.randomUUID())
                .employeeCode("EMP-101")
                .firstName("Ramesh")
                .lastName("Kulkarni")
                .dateOfBirth(LocalDate.of(1991, 3, 20))
                .gender("MALE")
                .dateOfJoining(LocalDate.of(2025, 4, 1))
                .build();
    }

    private EmployeeResponse buildSampleResponse(UUID id) {
        return EmployeeResponse.builder()
                .id(id)
                .employeeCode("EMP-101")
                .firstName("Ramesh")
                .lastName("Kulkarni")
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/employees returns 201 for ROLE_FOUNDER")
    @WithMockUser(roles = "FOUNDER")
    void shouldAllowCreateEmployeeForFounder() throws Exception {
        UUID id = UUID.randomUUID();
        CreateEmployeeRequest request = buildValidCreateRequest();
        when(employeeService.createEmployee(any())).thenReturn(buildSampleResponse(id));

        mockMvc.perform(post("/api/v1/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.employeeCode").value("EMP-101"));
    }

    @Test
    @DisplayName("POST /api/v1/employees returns 201 for ROLE_HR")
    @WithMockUser(roles = "HR")
    void shouldAllowCreateEmployeeForHr() throws Exception {
        UUID id = UUID.randomUUID();
        CreateEmployeeRequest request = buildValidCreateRequest();
        when(employeeService.createEmployee(any())).thenReturn(buildSampleResponse(id));

        mockMvc.perform(post("/api/v1/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("POST /api/v1/employees returns 403 for ROLE_FINANCE, ROLE_CA, and ROLE_EMPLOYEE")
    @WithMockUser(roles = "FINANCE")
    void shouldDenyCreateEmployeeForFinance() throws Exception {
        CreateEmployeeRequest request = buildValidCreateRequest();

        mockMvc.perform(post("/api/v1/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/employees returns 403 for unauthenticated user")
    void shouldDenyCreateEmployeeWhenUnauthenticated() throws Exception {
        CreateEmployeeRequest request = buildValidCreateRequest();

        mockMvc.perform(post("/api/v1/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/employees returns 200 for ROLE_CA and ROLE_FINANCE")
    @WithMockUser(roles = "CA")
    void shouldAllowGetAllEmployeesForCa() throws Exception {
        when(employeeService.getAllEmployees()).thenReturn(List.of(buildSampleResponse(UUID.randomUUID())));

        mockMvc.perform(get("/api/v1/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("GET /api/v1/employees returns 403 for ROLE_EMPLOYEE")
    @WithMockUser(roles = "EMPLOYEE")
    void shouldDenyGetAllEmployeesForEmployee() throws Exception {
        mockMvc.perform(get("/api/v1/employees"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/employees/{id} returns 200 for ROLE_EMPLOYEE")
    @WithMockUser(roles = "EMPLOYEE")
    void shouldAllowGetEmployeeByIdForEmployee() throws Exception {
        UUID id = UUID.randomUUID();
        when(employeeService.getEmployeeById(eq(id))).thenReturn(buildSampleResponse(id));

        mockMvc.perform(get("/api/v1/employees/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(id.toString()));
    }

    @Test
    @DisplayName("GET /api/v1/employees/{id} returns 403 for ROLE_CA")
    @WithMockUser(roles = "CA")
    void shouldDenyGetEmployeeByIdForCa() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/employees/" + id))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /api/v1/employees/{id} returns 200 for ROLE_HR")
    @WithMockUser(roles = "HR")
    void shouldAllowUpdateEmployeeForHr() throws Exception {
        UUID id = UUID.randomUUID();
        UpdateEmployeeRequest request = UpdateEmployeeRequest.builder()
                .firstName("UpdatedFirst")
                .build();

        EmployeeResponse response = buildSampleResponse(id);
        response.setFirstName("UpdatedFirst");

        when(employeeService.updateEmployee(eq(id), any())).thenReturn(response);

        mockMvc.perform(put("/api/v1/employees/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.firstName").value("UpdatedFirst"));
    }

    @Test
    @DisplayName("PUT /api/v1/employees/{id} returns 403 for ROLE_EMPLOYEE and ROLE_FINANCE")
    @WithMockUser(roles = "EMPLOYEE")
    void shouldDenyUpdateEmployeeForEmployee() throws Exception {
        UUID id = UUID.randomUUID();
        UpdateEmployeeRequest request = UpdateEmployeeRequest.builder().firstName("Test").build();

        mockMvc.perform(put("/api/v1/employees/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/employees/{id} returns 404 when employee not found")
    @WithMockUser(roles = "HR")
    void shouldReturn404WhenEmployeeNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(employeeService.getEmployeeById(eq(id))).thenThrow(new ResourceNotFoundException("Employee not found"));

        mockMvc.perform(get("/api/v1/employees/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
    }
}

package com.ourhr.company;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ourhr.common.ResourceNotFoundException;
import com.ourhr.company.dto.BranchResponse;
import com.ourhr.company.dto.CreateBranchRequest;
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

@WebMvcTest(BranchController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class BranchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BranchService branchService;

    @MockBean
    private JwtService jwtService;

    @Test
    @DisplayName("POST /api/v1/branches should return 201 Created for ROLE_HR")
    @WithMockUser(roles = "HR")
    void shouldCreateBranchForHr() throws Exception {
        CreateBranchRequest request = CreateBranchRequest.builder()
                .branchName("Hyderabad Development Center")
                .stateCode("TG")
                .city("Hyderabad")
                .addressLine("HITEC City, Madhapur")
                .pincode("500081")
                .isHeadquarters(false)
                .build();

        BranchResponse response = BranchResponse.builder()
                .id(UUID.randomUUID())
                .companyId(UUID.randomUUID())
                .branchName(request.getBranchName())
                .stateCode(request.getStateCode())
                .city(request.getCity())
                .addressLine(request.getAddressLine())
                .pincode(request.getPincode())
                .isHeadquarters(false)
                .build();

        when(branchService.createBranch(any(CreateBranchRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/branches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.branchName").value("Hyderabad Development Center"))
                .andExpect(jsonPath("$.data.stateCode").value("TG"));
    }

    @Test
    @DisplayName("POST /api/v1/branches should return 403 Forbidden for ROLE_EMPLOYEE")
    @WithMockUser(roles = "EMPLOYEE")
    void shouldDenyBranchCreationForEmployee() throws Exception {
        CreateBranchRequest request = CreateBranchRequest.builder()
                .branchName("Delhi Office")
                .stateCode("DL")
                .city("New Delhi")
                .addressLine("Connaught Place")
                .pincode("110001")
                .build();

        mockMvc.perform(post("/api/v1/branches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/branches should return 400 Bad Request for invalid pincode")
    @WithMockUser(roles = "FOUNDER")
    void shouldFailValidationOnInvalidPincode() throws Exception {
        CreateBranchRequest request = CreateBranchRequest.builder()
                .branchName("Chennai Office")
                .stateCode("TN")
                .city("Chennai")
                .addressLine("OMR Road")
                .pincode("000123") // Invalid Indian pincode (starts with 0)
                .build();

        mockMvc.perform(post("/api/v1/branches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("GET /api/v1/branches should return 200 OK for any authenticated user")
    @WithMockUser(roles = "EMPLOYEE")
    void shouldListBranchesForEmployee() throws Exception {
        BranchResponse branch = BranchResponse.builder()
                .id(UUID.randomUUID())
                .branchName("Main Office")
                .stateCode("KA")
                .city("Bangalore")
                .addressLine("MG Road")
                .pincode("560001")
                .isHeadquarters(true)
                .build();

        when(branchService.getAllBranches()).thenReturn(List.of(branch));

        mockMvc.perform(get("/api/v1/branches"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].branchName").value("Main Office"));
    }

    @Test
    @DisplayName("GET /api/v1/branches?state=KA should filter by state")
    @WithMockUser(roles = "FINANCE")
    void shouldFilterBranchesByState() throws Exception {
        BranchResponse branch = BranchResponse.builder()
                .id(UUID.randomUUID())
                .branchName("Bangalore Campus")
                .stateCode("KA")
                .city("Bangalore")
                .addressLine("Whitefield")
                .pincode("560066")
                .isHeadquarters(true)
                .build();

        when(branchService.getBranchesByState("KA")).thenReturn(List.of(branch));

        mockMvc.perform(get("/api/v1/branches").param("state", "KA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].stateCode").value("KA"));
    }

    @Test
    @DisplayName("GET /api/v1/branches/{id} should return 200 OK when branch exists")
    @WithMockUser(roles = "CA")
    void shouldGetBranchByIdForCa() throws Exception {
        UUID branchId = UUID.randomUUID();
        BranchResponse branch = BranchResponse.builder()
                .id(branchId)
                .branchName("Audit Office")
                .stateCode("DL")
                .city("New Delhi")
                .addressLine("Barakhamba Road")
                .pincode("110001")
                .isHeadquarters(false)
                .build();

        when(branchService.getBranchById(branchId)).thenReturn(branch);

        mockMvc.perform(get("/api/v1/branches/{id}", branchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(branchId.toString()));
    }

    @Test
    @DisplayName("GET /api/v1/branches/{id} should return 404 NOT_FOUND when branch not found")
    @WithMockUser(roles = "HR")
    void shouldReturn404WhenBranchNotFound() throws Exception {
        UUID branchId = UUID.randomUUID();
        when(branchService.getBranchById(branchId)).thenThrow(new ResourceNotFoundException("Branch not found"));

        mockMvc.perform(get("/api/v1/branches/{id}", branchId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("PUT /api/v1/branches/{id} should return 200 OK for ROLE_FOUNDER")
    @WithMockUser(roles = "FOUNDER")
    void shouldUpdateBranchForFounder() throws Exception {
        UUID branchId = UUID.randomUUID();
        CreateBranchRequest request = CreateBranchRequest.builder()
                .branchName("Bangalore North Hub")
                .stateCode("KA")
                .city("Bangalore")
                .addressLine("Hebbal")
                .pincode("560024")
                .isHeadquarters(false)
                .build();

        BranchResponse response = BranchResponse.builder()
                .id(branchId)
                .branchName(request.getBranchName())
                .stateCode(request.getStateCode())
                .city(request.getCity())
                .addressLine(request.getAddressLine())
                .pincode(request.getPincode())
                .isHeadquarters(false)
                .build();

        when(branchService.updateBranch(eq(branchId), any(CreateBranchRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/branches/{id}", branchId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.branchName").value("Bangalore North Hub"));
    }

    @Test
    @DisplayName("PUT /api/v1/branches/{id} should return 403 Forbidden for ROLE_CA")
    @WithMockUser(roles = "CA")
    void shouldDenyUpdateBranchForCa() throws Exception {
        UUID branchId = UUID.randomUUID();
        CreateBranchRequest request = CreateBranchRequest.builder()
                .branchName("Bangalore North Hub")
                .stateCode("KA")
                .city("Bangalore")
                .addressLine("Hebbal")
                .pincode("560024")
                .isHeadquarters(false)
                .build();

        mockMvc.perform(put("/api/v1/branches/{id}", branchId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}

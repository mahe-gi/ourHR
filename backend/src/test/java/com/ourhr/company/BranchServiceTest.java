package com.ourhr.company;

import com.ourhr.common.ResourceNotFoundException;
import com.ourhr.common.TenantContext;
import com.ourhr.company.dto.BranchResponse;
import com.ourhr.company.dto.CreateBranchRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BranchServiceTest {

    @Mock
    private BranchRepository branchRepository;

    @InjectMocks
    private BranchService branchService;

    private UUID tenantId;
    private Branch existingHq;
    private Branch branchKA;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);

        existingHq = Branch.builder()
                .branchName("Bangalore HQ")
                .stateCode("KA")
                .city("Bangalore")
                .addressLine("100 Feet Road, Indiranagar")
                .pincode("560038")
                .isHeadquarters(true)
                .build();
        existingHq.setId(UUID.randomUUID());
        existingHq.setCompanyId(tenantId);

        branchKA = Branch.builder()
                .branchName("Electronic City Branch")
                .stateCode("KA")
                .city("Bangalore")
                .addressLine("Phase 1, Electronic City")
                .pincode("560100")
                .isHeadquarters(false)
                .build();
        branchKA.setId(UUID.randomUUID());
        branchKA.setCompanyId(tenantId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("createBranch with isHeadquarters=true should unset existing HQ and set new branch as HQ")
    void shouldCreateBranchAndUnsetPreviousHq() {
        CreateBranchRequest request = CreateBranchRequest.builder()
                .branchName("Mumbai Corporate Office")
                .stateCode("MH")
                .city("Mumbai")
                .addressLine("BKC Bandra")
                .pincode("400051")
                .isHeadquarters(true)
                .build();

        when(branchRepository.findByCompanyIdAndIsHeadquartersTrue(tenantId)).thenReturn(Optional.of(existingHq));
        when(branchRepository.save(any(Branch.class))).thenAnswer(invocation -> {
            Branch b = invocation.getArgument(0);
            if (b.getId() == null) {
                b.setId(UUID.randomUUID());
            }
            return b;
        });

        BranchResponse response = branchService.createBranch(request);

        assertNotNull(response);
        assertEquals("Mumbai Corporate Office", response.getBranchName());
        assertEquals("MH", response.getStateCode());
        assertTrue(response.getIsHeadquarters());
        assertEquals(tenantId, response.getCompanyId());

        assertFalse(existingHq.getIsHeadquarters());
        verify(branchRepository).save(existingHq);
    }

    @Test
    @DisplayName("createBranch with isHeadquarters=false should not modify existing HQ")
    void shouldCreateRegularBranchWithoutModifyingHq() {
        CreateBranchRequest request = CreateBranchRequest.builder()
                .branchName("Pune Tech Park")
                .stateCode("MH")
                .city("Pune")
                .addressLine("Hinjewadi Phase 2")
                .pincode("411057")
                .isHeadquarters(false)
                .build();

        when(branchRepository.save(any(Branch.class))).thenAnswer(invocation -> {
            Branch b = invocation.getArgument(0);
            b.setId(UUID.randomUUID());
            return b;
        });

        BranchResponse response = branchService.createBranch(request);

        assertNotNull(response);
        assertEquals("Pune Tech Park", response.getBranchName());
        assertFalse(response.getIsHeadquarters());
        verify(branchRepository, never()).findByCompanyIdAndIsHeadquartersTrue(any());
    }

    @Test
    @DisplayName("createBranch should throw IllegalArgumentException when tenant context is missing")
    void shouldThrowWhenCreatingBranchWithoutTenant() {
        TenantContext.clear();

        CreateBranchRequest request = CreateBranchRequest.builder()
                .branchName("Delhi Branch")
                .stateCode("DL")
                .city("New Delhi")
                .addressLine("Connaught Place")
                .pincode("110001")
                .build();

        assertThrows(IllegalArgumentException.class, () -> branchService.createBranch(request));
    }

    @Test
    @DisplayName("getAllBranches should return all branches for current tenant")
    void shouldGetAllBranchesForTenant() {
        when(branchRepository.findByCompanyId(tenantId)).thenReturn(List.of(existingHq, branchKA));

        List<BranchResponse> responses = branchService.getAllBranches();

        assertEquals(2, responses.size());
        assertEquals("Bangalore HQ", responses.get(0).getBranchName());
        assertEquals("Electronic City Branch", responses.get(1).getBranchName());
    }

    @Test
    @DisplayName("getBranchById should return branch if owned by current tenant")
    void shouldGetBranchByIdForTenant() {
        UUID branchId = existingHq.getId();
        when(branchRepository.findByIdAndCompanyId(branchId, tenantId)).thenReturn(Optional.of(existingHq));

        BranchResponse response = branchService.getBranchById(branchId);

        assertNotNull(response);
        assertEquals(branchId, response.getId());
        assertEquals("Bangalore HQ", response.getBranchName());
    }

    @Test
    @DisplayName("getBranchById should throw ResourceNotFoundException when branch belongs to another tenant")
    void shouldThrowWhenBranchBelongsToDifferentTenant() {
        UUID branchId = UUID.randomUUID();
        when(branchRepository.findByIdAndCompanyId(branchId, tenantId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> branchService.getBranchById(branchId));
    }

    @Test
    @DisplayName("getBranchesByState should return state-specific branches")
    void shouldGetBranchesByState() {
        when(branchRepository.findByCompanyIdAndStateCode(tenantId, "KA")).thenReturn(List.of(existingHq, branchKA));

        List<BranchResponse> responses = branchService.getBranchesByState("ka");

        assertEquals(2, responses.size());
        verify(branchRepository).findByCompanyIdAndStateCode(tenantId, "KA");
    }

    @Test
    @DisplayName("updateBranch should update details and switch HQ if requested")
    void shouldUpdateBranchAndSwitchHq() {
        UUID branchId = branchKA.getId();
        when(branchRepository.findByIdAndCompanyId(branchId, tenantId)).thenReturn(Optional.of(branchKA));
        when(branchRepository.findByCompanyIdAndIsHeadquartersTrue(tenantId)).thenReturn(Optional.of(existingHq));
        when(branchRepository.save(any(Branch.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateBranchRequest updateRequest = CreateBranchRequest.builder()
                .branchName("Bangalore Prime Campus")
                .stateCode("KA")
                .city("Bangalore")
                .addressLine("Outer Ring Road, Bellandur")
                .pincode("560103")
                .isHeadquarters(true)
                .build();

        BranchResponse response = branchService.updateBranch(branchId, updateRequest);

        assertNotNull(response);
        assertEquals("Bangalore Prime Campus", response.getBranchName());
        assertEquals("Outer Ring Road, Bellandur", response.getAddressLine());
        assertTrue(response.getIsHeadquarters());

        assertFalse(existingHq.getIsHeadquarters());
        verify(branchRepository).save(existingHq);
    }
}

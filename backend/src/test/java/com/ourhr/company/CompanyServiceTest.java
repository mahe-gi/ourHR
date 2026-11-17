package com.ourhr.company;

import com.ourhr.common.ResourceNotFoundException;
import com.ourhr.common.TenantContext;
import com.ourhr.company.dto.CompanyResponse;
import com.ourhr.company.dto.UpdateCompanyRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {

    @Mock
    private CompanyRepository companyRepository;

    @InjectMocks
    private CompanyService companyService;

    private UUID tenantId;
    private Company sampleCompany;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);

        sampleCompany = Company.builder()
                .id(tenantId)
                .legalName("Acme Technologies Pvt Ltd")
                .tradeName("Acme Tech")
                .panNumber("ABCDE1234F")
                .tanNumber("BLRA12345E")
                .gstin("29ABCDE1234F1Z5")
                .pfEstablishmentCode("KNBLR1234567000")
                .esicCode("53000123450000001")
                .lin("1234567890")
                .signatoryName("Rahul Sharma")
                .signatoryDesignation("Managing Director")
                .build();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("getCurrentCompany should return company profile for current tenant")
    void shouldGetCurrentCompany() {
        when(companyRepository.findById(tenantId)).thenReturn(Optional.of(sampleCompany));

        CompanyResponse response = companyService.getCurrentCompany();

        assertNotNull(response);
        assertEquals(tenantId, response.getId());
        assertEquals("Acme Technologies Pvt Ltd", response.getLegalName());
        assertEquals("ABCDE1234F", response.getPanNumber());
        assertEquals("Rahul Sharma", response.getSignatoryName());
    }

    @Test
    @DisplayName("getCurrentCompany should throw ResourceNotFoundException when company not found")
    void shouldThrowWhenCompanyNotFound() {
        when(companyRepository.findById(tenantId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> companyService.getCurrentCompany());
    }

    @Test
    @DisplayName("getCurrentCompany should throw IllegalArgumentException when tenant context is missing")
    void shouldThrowWhenTenantContextMissing() {
        TenantContext.clear();

        assertThrows(IllegalArgumentException.class, () -> companyService.getCurrentCompany());
    }

    @Test
    @DisplayName("updateCurrentCompany should update company settings")
    void shouldUpdateCompanySettings() {
        when(companyRepository.findById(tenantId)).thenReturn(Optional.of(sampleCompany));
        when(companyRepository.save(any(Company.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateCompanyRequest request = UpdateCompanyRequest.builder()
                .tradeName("Acme Global")
                .signatoryName("Priya Verma")
                .signatoryDesignation("Chief Executive Officer")
                .gstin("29ABCDE1234F2Z6")
                .build();

        CompanyResponse response = companyService.updateCurrentCompany(request);

        assertNotNull(response);
        assertEquals("Acme Global", response.getTradeName());
        assertEquals("Priya Verma", response.getSignatoryName());
        assertEquals("Chief Executive Officer", response.getSignatoryDesignation());
        assertEquals("29ABCDE1234F2Z6", response.getGstin());
        verify(companyRepository).save(any(Company.class));
    }

    @Test
    @DisplayName("updateCurrentCompany should throw IllegalArgumentException when signatory name is blank")
    void shouldThrowWhenSignatoryNameIsBlank() {
        when(companyRepository.findById(tenantId)).thenReturn(Optional.of(sampleCompany));

        UpdateCompanyRequest request = UpdateCompanyRequest.builder()
                .signatoryName("   ")
                .build();

        assertThrows(IllegalArgumentException.class, () -> companyService.updateCurrentCompany(request));
    }
}

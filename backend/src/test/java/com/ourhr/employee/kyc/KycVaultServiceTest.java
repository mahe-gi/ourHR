package com.ourhr.employee.kyc;

import com.ourhr.common.ResourceNotFoundException;
import com.ourhr.common.TenantContext;
import com.ourhr.employee.Employee;
import com.ourhr.employee.EmployeeRepository;
import com.ourhr.employee.kyc.dto.CreateKycVaultRequest;
import com.ourhr.employee.kyc.dto.KycVaultResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KycVaultServiceTest {

    @Mock
    private EmployeeKycVaultRepository kycVaultRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private KycVaultService kycVaultService;

    private UUID tenantId;
    private UUID employeeId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        employeeId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Sensitive ID Redaction Law: Raw Aadhaar input is masked to [Aadhaar Redacted] and bank account is masked")
    void shouldRedactSensitiveIdsWhenSavingKycVault() {
        Employee employee = Employee.builder()
                .employeeCode("EMP-001")
                .firstName("Mahesh")
                .lastName("Chavan")
                .build();
        employee.setId(employeeId);
        employee.setCompanyId(tenantId);

        when(employeeRepository.findByIdAndCompanyId(eq(employeeId), eq(tenantId)))
                .thenReturn(Optional.of(employee));
        when(kycVaultRepository.findByCompanyIdAndEmployeeId(eq(tenantId), eq(employeeId)))
                .thenReturn(Optional.empty());

        when(kycVaultRepository.save(any(EmployeeKycVault.class)))
                .thenAnswer(invocation -> {
                    EmployeeKycVault vault = invocation.getArgument(0);
                    vault.setId(UUID.randomUUID());
                    return vault;
                });

        CreateKycVaultRequest request = CreateKycVaultRequest.builder()
                .employeeId(employeeId)
                .panNumber("ABCDE1234F")
                .aadhaarNumber("999988887777")
                .uan("100900800700")
                .esicIpNumber("12345678901234567")
                .bankAccountNumber("98765432101234")
                .bankIfsc("HDFC0001234")
                .bankName("HDFC Bank")
                .taxRegimePreference("NEW")
                .build();

        KycVaultResponse response = kycVaultService.saveKycVault(employeeId, request);

        ArgumentCaptor<EmployeeKycVault> captor = ArgumentCaptor.forClass(EmployeeKycVault.class);
        verify(kycVaultRepository).save(captor.capture());
        EmployeeKycVault savedEntity = captor.getValue();

        assertThat(savedEntity.getMaskedAadhaarToken()).isEqualTo("[Aadhaar Redacted]");
        assertThat(savedEntity.getPanNumber()).isEqualTo("ABCDE1234F");
        assertThat(savedEntity.getCompanyId()).isEqualTo(tenantId);
        assertThat(savedEntity.getEmployeeId()).isEqualTo(employeeId);

        assertThat(response.getMaskedAadhaarToken()).isEqualTo("[Aadhaar Redacted]");
        assertThat(response.getBankAccountNumberMasked()).isEqualTo("******1234");
        assertThat(response.getPanNumber()).isEqualTo("ABCDE1234F");
    }

    @Test
    @DisplayName("Updates existing KYC vault record when present")
    void shouldUpdateExistingKycVaultRecord() {
        Employee employee = Employee.builder().build();
        employee.setId(employeeId);
        employee.setCompanyId(tenantId);

        EmployeeKycVault existing = EmployeeKycVault.builder()
                .employeeId(employeeId)
                .panNumber("OLDPA1234F")
                .bankAccountNumber("111122223333")
                .bankIfsc("SBIN0001234")
                .bankName("State Bank of India")
                .build();
        existing.setId(UUID.randomUUID());
        existing.setCompanyId(tenantId);

        when(employeeRepository.findByIdAndCompanyId(eq(employeeId), eq(tenantId)))
                .thenReturn(Optional.of(employee));
        when(kycVaultRepository.findByCompanyIdAndEmployeeId(eq(tenantId), eq(employeeId)))
                .thenReturn(Optional.of(existing));
        when(kycVaultRepository.save(any(EmployeeKycVault.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CreateKycVaultRequest request = CreateKycVaultRequest.builder()
                .employeeId(employeeId)
                .panNumber("NEWPA1234F")
                .aadhaarNumber("999988887777")
                .bankAccountNumber("999988884444")
                .bankIfsc("ICIC0001234")
                .bankName("ICICI Bank")
                .taxRegimePreference("OLD")
                .build();

        KycVaultResponse response = kycVaultService.saveKycVault(employeeId, request);

        assertThat(response.getPanNumber()).isEqualTo("NEWPA1234F");
        assertThat(response.getMaskedAadhaarToken()).isEqualTo("[Aadhaar Redacted]");
        assertThat(response.getBankAccountNumberMasked()).isEqualTo("******4444");
        assertThat(response.getBankName()).isEqualTo("ICICI Bank");
        assertThat(response.getTaxRegimePreference()).isEqualTo("OLD");
    }

    @Test
    @DisplayName("Multi-tenant isolation: Throws ResourceNotFoundException when employee belongs to another company")
    void shouldThrowWhenEmployeeNotFoundInCurrentTenant() {
        when(employeeRepository.findByIdAndCompanyId(eq(employeeId), eq(tenantId)))
                .thenReturn(Optional.empty());

        CreateKycVaultRequest request = CreateKycVaultRequest.builder()
                .panNumber("ABCDE1234F")
                .bankAccountNumber("1234567890")
                .bankIfsc("HDFC0001234")
                .bankName("HDFC Bank")
                .build();

        assertThatThrownBy(() -> kycVaultService.saveKycVault(employeeId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Employee not found with id: " + employeeId);

        verify(kycVaultRepository, never()).save(any());
    }

    @Test
    @DisplayName("Missing TenantContext throws IllegalArgumentException")
    void shouldThrowWhenTenantContextMissing() {
        TenantContext.clear();

        CreateKycVaultRequest request = CreateKycVaultRequest.builder()
                .panNumber("ABCDE1234F")
                .build();

        assertThatThrownBy(() -> kycVaultService.saveKycVault(employeeId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tenant context is required");

        assertThatThrownBy(() -> kycVaultService.getKycVault(employeeId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tenant context is required");
    }

    @Test
    @DisplayName("getKycVault retrieves redacted details for tenant")
    void shouldGetKycVaultWithRedactedFields() {
        EmployeeKycVault vault = EmployeeKycVault.builder()
                .employeeId(employeeId)
                .panNumber("ABCDE1234F")
                .maskedAadhaarToken("[Aadhaar Redacted]")
                .uan("100900800700")
                .esicIpNumber("12345678901234567")
                .bankAccountNumber("123456789012")
                .bankIfsc("HDFC0001234")
                .bankName("HDFC Bank")
                .taxRegimePreference("NEW")
                .build();
        vault.setId(UUID.randomUUID());
        vault.setCompanyId(tenantId);

        when(kycVaultRepository.findByCompanyIdAndEmployeeId(eq(tenantId), eq(employeeId)))
                .thenReturn(Optional.of(vault));

        KycVaultResponse response = kycVaultService.getKycVault(employeeId);

        assertThat(response).isNotNull();
        assertThat(response.getPanNumber()).isEqualTo("ABCDE1234F");
        assertThat(response.getMaskedAadhaarToken()).isEqualTo("[Aadhaar Redacted]");
        assertThat(response.getBankAccountNumberMasked()).isEqualTo("******9012");
        assertThat(response.getUan()).isEqualTo("100900800700");
    }

    @Test
    @DisplayName("getKycVault throws ResourceNotFoundException when vault does not exist")
    void shouldThrowWhenKycVaultNotFound() {
        when(kycVaultRepository.findByCompanyIdAndEmployeeId(eq(tenantId), eq(employeeId)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> kycVaultService.getKycVault(employeeId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("KYC vault not found for employee: " + employeeId);
    }

    @Test
    @DisplayName("maskBankAccount masks account number preserving last 4 digits")
    void shouldCorrectlyMaskBankAccountNumbers() {
        assertThat(KycVaultService.maskBankAccount("123456789012")).isEqualTo("******9012");
        assertThat(KycVaultService.maskBankAccount("1234")).isEqualTo("******1234");
        assertThat(KycVaultService.maskBankAccount("99")).isEqualTo("******99");
        assertThat(KycVaultService.maskBankAccount("")).isEqualTo("");
        assertThat(KycVaultService.maskBankAccount(null)).isEqualTo("");
        assertThat(KycVaultService.maskBankAccount("   9876543210   ")).isEqualTo("******3210");
    }
}

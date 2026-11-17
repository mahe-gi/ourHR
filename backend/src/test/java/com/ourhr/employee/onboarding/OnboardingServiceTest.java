package com.ourhr.employee.onboarding;

import com.ourhr.common.ResourceNotFoundException;
import com.ourhr.common.TenantContext;
import com.ourhr.employee.Employee;
import com.ourhr.employee.EmployeeRepository;
import com.ourhr.employee.EmployeeService;
import com.ourhr.employee.dto.UpdateEmployeeRequest;
import com.ourhr.employee.kyc.EmployeeKycVault;
import com.ourhr.employee.kyc.EmployeeKycVaultRepository;
import com.ourhr.employee.kyc.KycVaultService;
import com.ourhr.employee.kyc.dto.CreateKycVaultRequest;
import com.ourhr.employee.onboarding.dto.OnboardingLinkResponse;
import com.ourhr.employee.onboarding.dto.OnboardingStatusResponse;
import com.ourhr.employee.onboarding.dto.OnboardingSubmissionRequest;
import com.ourhr.employee.salary.PreviousEmployerIncome;
import com.ourhr.employee.salary.PreviousEmployerIncomeRepository;
import com.ourhr.employee.salary.PreviousEmployerIncomeService;
import com.ourhr.employee.salary.dto.SubmitForm12BRequest;
import com.ourhr.security.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OnboardingServiceTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private EmployeeService employeeService;

    @Mock
    private KycVaultService kycVaultService;

    @Mock
    private PreviousEmployerIncomeService previousEmployerIncomeService;

    @Mock
    private EmployeeKycVaultRepository kycVaultRepository;

    @Mock
    private PreviousEmployerIncomeRepository previousEmployerIncomeRepository;

    @InjectMocks
    private OnboardingService onboardingService;

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
    @DisplayName("Generate onboarding link with token and valid expiration")
    void shouldGenerateOnboardingLinkSuccessfully() {
        Employee employee = Employee.builder()
                .employeeCode("EMP-100")
                .firstName("Suresh")
                .lastName("Patil")
                .build();
        employee.setId(employeeId);
        employee.setCompanyId(tenantId);

        when(employeeRepository.findByIdAndCompanyId(eq(employeeId), eq(tenantId)))
                .thenReturn(Optional.of(employee));
        when(jwtService.generateOnboardingToken(eq(employeeId), eq(tenantId), anyLong()))
                .thenReturn("mock-jwt-onboarding-token");

        OnboardingLinkResponse response = onboardingService.generateOnboardingLink(employeeId, 14);

        assertThat(response).isNotNull();
        assertThat(response.getEmployeeId()).isEqualTo(employeeId);
        assertThat(response.getToken()).isEqualTo("mock-jwt-onboarding-token");
        assertThat(response.getOnboardingUrl()).isEqualTo("/onboarding?token=mock-jwt-onboarding-token");
        assertThat(response.getExpiresAt()).isAfter(Instant.now());

        verify(jwtService).generateOnboardingToken(eq(employeeId), eq(tenantId), eq(14L * 24 * 60 * 60 * 1000L));
    }

    @Test
    @DisplayName("Generate onboarding link defaults to 7 days expiration when input is zero or negative")
    void shouldDefaultToSevenDaysExpiration() {
        Employee employee = Employee.builder().build();
        employee.setId(employeeId);
        employee.setCompanyId(tenantId);

        when(employeeRepository.findByIdAndCompanyId(eq(employeeId), eq(tenantId)))
                .thenReturn(Optional.of(employee));
        when(jwtService.generateOnboardingToken(eq(employeeId), eq(tenantId), anyLong()))
                .thenReturn("token");

        onboardingService.generateOnboardingLink(employeeId, 0);

        verify(jwtService).generateOnboardingToken(eq(employeeId), eq(tenantId), eq(7L * 24 * 60 * 60 * 1000L));
    }

    @Test
    @DisplayName("Generate onboarding link throws ResourceNotFoundException when employee not found")
    void shouldThrowWhenEmployeeNotFoundForLinkGeneration() {
        when(employeeRepository.findByIdAndCompanyId(eq(employeeId), eq(tenantId)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> onboardingService.generateOnboardingLink(employeeId, 7))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Employee not found with id: " + employeeId);
    }

    @Test
    @DisplayName("Submit onboarding updates employee profile, saves KYC vault, and records Form 12B")
    void shouldSubmitOnboardingSuccessfully() {
        String token = "valid-onboarding-token";
        when(jwtService.isTokenValid(token)).thenReturn(true);
        when(jwtService.extractCompanyId(token)).thenReturn(tenantId);
        when(jwtService.extractEmployeeId(token)).thenReturn(employeeId);

        Employee employee = Employee.builder()
                .employeeCode("EMP-200")
                .firstName("Anita")
                .lastName("Deshmukh")
                .build();
        employee.setId(employeeId);
        employee.setCompanyId(tenantId);

        when(employeeRepository.findByIdAndCompanyId(eq(employeeId), eq(tenantId)))
                .thenReturn(Optional.of(employee));

        UpdateEmployeeRequest personalDetails = UpdateEmployeeRequest.builder()
                .firstName("Anita")
                .lastName("Deshmukh")
                .build();

        CreateKycVaultRequest kycDetails = CreateKycVaultRequest.builder()
                .panNumber("ABCDE1234F")
                .aadhaarNumber("123456789012")
                .bankAccountNumber("1234567890123")
                .bankIfsc("SBIN0001234")
                .bankName("SBI")
                .build();

        SubmitForm12BRequest previousEmployerDetails = SubmitForm12BRequest.builder()
                .financialYear("2025-2026")
                .grossSalary(new BigDecimal("250000.00"))
                .build();

        OnboardingSubmissionRequest submission = OnboardingSubmissionRequest.builder()
                .token(token)
                .personalDetails(personalDetails)
                .kycDetails(kycDetails)
                .previousEmployerDetails(previousEmployerDetails)
                .build();

        OnboardingStatusResponse response = onboardingService.submitOnboarding(submission);

        assertThat(response).isNotNull();
        assertThat(response.getEmployeeId()).isEqualTo(employeeId);
        assertThat(response.getOnboardingStatus()).isEqualTo("COMPLETED");
        assertThat(response.isKycSubmitted()).isTrue();
        assertThat(response.isForm12bSubmitted()).isTrue();

        verify(employeeService).updateEmployee(eq(employeeId), eq(personalDetails));
        verify(kycVaultService).saveKycVault(eq(employeeId), eq(kycDetails));
        verify(previousEmployerIncomeService).savePreviousIncome(eq(previousEmployerDetails));
    }

    @Test
    @DisplayName("Submit onboarding rejects null or invalid onboarding tokens")
    void shouldRejectInvalidOnboardingToken() {
        OnboardingSubmissionRequest nullTokenRequest = OnboardingSubmissionRequest.builder()
                .token(null)
                .build();

        assertThatThrownBy(() -> onboardingService.submitOnboarding(nullTokenRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Onboarding invitation token is required");

        OnboardingSubmissionRequest invalidTokenRequest = OnboardingSubmissionRequest.builder()
                .token("expired-invalid-token")
                .build();

        when(jwtService.isTokenValid("expired-invalid-token")).thenReturn(false);

        assertThatThrownBy(() -> onboardingService.submitOnboarding(invalidTokenRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid or expired onboarding token");
    }

    @Test
    @DisplayName("getOnboardingStatus returns COMPLETED when KYC is submitted")
    void shouldReturnCompletedStatusWhenKycPresent() {
        Employee employee = Employee.builder()
                .employeeCode("EMP-300")
                .firstName("Rohan")
                .lastName("Verma")
                .build();
        employee.setId(employeeId);
        employee.setCompanyId(tenantId);

        EmployeeKycVault vault = EmployeeKycVault.builder()
                .employeeId(employeeId)
                .build();
        vault.setUpdatedAt(Instant.now());

        when(employeeRepository.findByIdAndCompanyId(eq(employeeId), eq(tenantId)))
                .thenReturn(Optional.of(employee));
        when(kycVaultRepository.findByCompanyIdAndEmployeeId(eq(tenantId), eq(employeeId)))
                .thenReturn(Optional.of(vault));
        when(previousEmployerIncomeRepository.findByCompanyIdAndEmployeeId(eq(tenantId), eq(employeeId)))
                .thenReturn(Optional.of(new PreviousEmployerIncome()));

        OnboardingStatusResponse status = onboardingService.getOnboardingStatus(employeeId);

        assertThat(status.getOnboardingStatus()).isEqualTo("COMPLETED");
        assertThat(status.isKycSubmitted()).isTrue();
        assertThat(status.isForm12bSubmitted()).isTrue();
    }

    @Test
    @DisplayName("getOnboardingStatus returns PENDING when KYC is not yet submitted")
    void shouldReturnPendingStatusWhenKycAbsent() {
        Employee employee = Employee.builder()
                .employeeCode("EMP-300")
                .firstName("Rohan")
                .lastName("Verma")
                .build();
        employee.setId(employeeId);
        employee.setCompanyId(tenantId);

        when(employeeRepository.findByIdAndCompanyId(eq(employeeId), eq(tenantId)))
                .thenReturn(Optional.of(employee));
        when(kycVaultRepository.findByCompanyIdAndEmployeeId(eq(tenantId), eq(employeeId)))
                .thenReturn(Optional.empty());
        when(previousEmployerIncomeRepository.findByCompanyIdAndEmployeeId(eq(tenantId), eq(employeeId)))
                .thenReturn(Optional.empty());

        OnboardingStatusResponse status = onboardingService.getOnboardingStatus(employeeId);

        assertThat(status.getOnboardingStatus()).isEqualTo("PENDING");
        assertThat(status.isKycSubmitted()).isFalse();
        assertThat(status.isForm12bSubmitted()).isFalse();
    }
}

package com.ourhr.payroll;

import com.ourhr.common.TenantContext;
import com.ourhr.employee.Employee;
import com.ourhr.employee.EmployeeRepository;
import com.ourhr.employee.kyc.KycVaultService;
import com.ourhr.employee.kyc.dto.KycVaultResponse;
import com.ourhr.payroll.dto.ConsultantPayoutRequest;
import com.ourhr.payroll.dto.ConsultantPayoutResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsultantPayoutServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private KycVaultService kycVaultService;

    @InjectMocks
    private ConsultantPayoutService consultantPayoutService;

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
    @DisplayName("Should process consultant Section 194J payout correctly")
    void shouldProcessConsultant194JPayout() {
        Employee consultant = Employee.builder()
                .firstName("Anita")
                .lastName("Rao")
                .workerType("CONSULTANT_194J")
                .build();
        consultant.setId(employeeId);
        consultant.setCompanyId(tenantId);

        KycVaultResponse kyc = KycVaultResponse.builder()
                .panNumber("ABCDE1234F")
                .build();

        when(employeeRepository.findByIdAndCompanyId(employeeId, tenantId)).thenReturn(Optional.of(consultant));
        when(kycVaultService.getKycVault(employeeId)).thenReturn(kyc);

        ConsultantPayoutRequest request = ConsultantPayoutRequest.builder()
                .consultantEmployeeId(employeeId)
                .grossAmount(new BigDecimal("100000.00"))
                .tdsSection("194J")
                .tdsRate(new BigDecimal("10.0"))
                .description("Technical consultancy fee")
                .build();

        ConsultantPayoutResponse response = consultantPayoutService.processConsultantPayout(request);

        assertThat(response).isNotNull();
        assertThat(response.getConsultantName()).isEqualTo("Anita Rao");
        assertThat(response.getPanNumber()).isEqualTo("ABCDE1234F");
        assertThat(response.getGrossAmount()).isEqualByComparingTo(new BigDecimal("100000.00"));
        assertThat(response.getTdsAmount()).isEqualByComparingTo(new BigDecimal("10000.00"));
        assertThat(response.getNetPayable()).isEqualByComparingTo(new BigDecimal("90000.00"));
        assertThat(response.getStatus()).isEqualTo("PROCESSED");
    }

    @Test
    @DisplayName("Should process intern payout under Section 194J/194C")
    void shouldProcessInternPayout() {
        Employee intern = Employee.builder()
                .firstName("Suresh")
                .lastName("Verma")
                .workerType("INTERN")
                .build();
        intern.setId(employeeId);
        intern.setCompanyId(tenantId);

        when(employeeRepository.findByIdAndCompanyId(employeeId, tenantId)).thenReturn(Optional.of(intern));
        when(kycVaultService.getKycVault(employeeId)).thenReturn(null);

        ConsultantPayoutRequest request = ConsultantPayoutRequest.builder()
                .consultantEmployeeId(employeeId)
                .grossAmount(new BigDecimal("25000.00"))
                .tdsSection("194J")
                .tdsRate(new BigDecimal("10.0"))
                .description("Intern stipend")
                .build();

        ConsultantPayoutResponse response = consultantPayoutService.processConsultantPayout(request);

        assertThat(response).isNotNull();
        assertThat(response.getGrossAmount()).isEqualByComparingTo(new BigDecimal("25000.00"));
        assertThat(response.getTdsAmount()).isEqualByComparingTo(new BigDecimal("2500.00"));
        assertThat(response.getNetPayable()).isEqualByComparingTo(new BigDecimal("22500.00"));
    }

    @Test
    @DisplayName("Should reject payout calculation if worker type is regular employee")
    void shouldRejectRegularEmployee() {
        Employee regular = Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .workerType("REGULAR")
                .build();
        regular.setId(employeeId);
        regular.setCompanyId(tenantId);

        when(employeeRepository.findByIdAndCompanyId(employeeId, tenantId)).thenReturn(Optional.of(regular));

        ConsultantPayoutRequest request = ConsultantPayoutRequest.builder()
                .consultantEmployeeId(employeeId)
                .grossAmount(new BigDecimal("50000.00"))
                .tdsSection("194J")
                .tdsRate(new BigDecimal("10.0"))
                .build();

        assertThatThrownBy(() -> consultantPayoutService.processConsultantPayout(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Employee worker type must be CONSULTANT_194J or INTERN");
    }
}

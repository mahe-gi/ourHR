package com.ourhr.exporter.ca;

import com.ourhr.common.TenantContext;
import com.ourhr.company.Branch;
import com.ourhr.company.BranchRepository;
import com.ourhr.employee.Employee;
import com.ourhr.employee.EmployeeRepository;
import com.ourhr.employee.kyc.EmployeeKycVault;
import com.ourhr.employee.kyc.EmployeeKycVaultRepository;
import com.ourhr.exporter.bank.BankExporterService;
import com.ourhr.exporter.ca.dto.CaComplianceSummaryResponse;
import com.ourhr.exporter.ca.dto.CaPtReturnSummaryResponse;
import com.ourhr.exporter.ca.dto.CaTaxAuditSummaryResponse;
import com.ourhr.exporter.ecr.EcrExporterService;
import com.ourhr.payroll.PayrollRun;
import com.ourhr.payroll.PayrollRunRepository;
import com.ourhr.payroll.PayslipLineItem;
import com.ourhr.payroll.PayslipLineItemRepository;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CaPortalServiceTest {

    @Mock
    private PayrollRunRepository payrollRunRepository;
    @Mock
    private PayslipLineItemRepository payslipLineItemRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private EmployeeKycVaultRepository employeeKycVaultRepository;
    @Mock
    private BranchRepository branchRepository;
    @Mock
    private EcrExporterService ecrExporterService;
    @Mock
    private BankExporterService bankExporterService;

    @InjectMocks
    private CaPortalService caPortalService;

    private UUID tenantId;
    private UUID payrollRunId;
    private UUID emp1Id;
    private UUID emp2Id;
    private UUID branchKaId;
    private UUID branchMhId;
    private PayrollRun payrollRun;
    private Employee empRegular;
    private Employee empConsultant;
    private Branch branchKa;
    private Branch branchMh;
    private EmployeeKycVault kyc1;
    private EmployeeKycVault kyc2;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        payrollRunId = UUID.randomUUID();
        emp1Id = UUID.randomUUID();
        emp2Id = UUID.randomUUID();
        branchKaId = UUID.randomUUID();
        branchMhId = UUID.randomUUID();

        TenantContext.setTenantId(tenantId);

        payrollRun = PayrollRun.builder()
                .payrollMonth(11)
                .payrollYear(2026)
                .runStatus("DISBURSED")
                .totalGrossPay(new BigDecimal("150000.00"))
                .totalNetPay(new BigDecimal("132000.00"))
                .totalPfLiability(new BigDecimal("7200.00"))
                .totalEsicLiability(new BigDecimal("1500.00"))
                .totalPtLiability(new BigDecimal("400.00"))
                .totalTdsLiability(new BigDecimal("12000.00"))
                .ecrTxtFileUrl("https://storage/ecr.txt")
                .bankPayoutFileUrl("https://storage/bank.csv")
                .approvedAt(Instant.now())
                .build();
        payrollRun.setId(payrollRunId);
        payrollRun.setCompanyId(tenantId);

        branchKa = Branch.builder().stateCode("KA").branchName("Bengaluru Tech Hub").build();
        branchKa.setId(branchKaId);

        branchMh = Branch.builder().stateCode("MH").branchName("Mumbai Sales Hub").build();
        branchMh.setId(branchMhId);

        empRegular = Employee.builder()
                .branchId(branchKaId)
                .employeeCode("EMP01")
                .firstName("Rohan")
                .lastName("Verma")
                .workerType("REGULAR")
                .build();
        empRegular.setId(emp1Id);
        empRegular.setCompanyId(tenantId);

        empConsultant = Employee.builder()
                .branchId(branchMhId)
                .employeeCode("CON01")
                .firstName("Dr.")
                .lastName("Mehta")
                .workerType("CONSULTANT_194J")
                .build();
        empConsultant.setId(emp2Id);
        empConsultant.setCompanyId(tenantId);

        kyc1 = EmployeeKycVault.builder()
                .employeeId(emp1Id)
                .panNumber("ABCDE1111A")
                .taxRegimePreference("NEW")
                .build();

        kyc2 = EmployeeKycVault.builder()
                .employeeId(emp2Id)
                .panNumber("ABCDE2222B")
                .taxRegimePreference("OLD")
                .build();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("CA compliance summary returns accurate figures and generation status")
    void testGetComplianceSummary() {
        when(payrollRunRepository.findByCompanyIdAndId(tenantId, payrollRunId))
                .thenReturn(Optional.of(payrollRun));

        PayslipLineItem item1 = PayslipLineItem.builder()
                .payrollRunId(payrollRunId)
                .employeeId(emp1Id)
                .itemCategory("EARNING")
                .itemName("Basic Pay")
                .itemAmount(new BigDecimal("80000.00"))
                .build();

        when(payslipLineItemRepository.findByCompanyIdAndPayrollRunId(tenantId, payrollRunId))
                .thenReturn(List.of(item1));

        CaComplianceSummaryResponse response = caPortalService.getComplianceSummary(payrollRunId);

        assertThat(response).isNotNull();
        assertThat(response.getPayrollRunId()).isEqualTo(payrollRunId);
        assertThat(response.getPayrollMonth()).isEqualTo(11);
        assertThat(response.getPayrollYear()).isEqualTo(2026);
        assertThat(response.getTotalEmployeesCovered()).isEqualTo(1);
        assertThat(response.getTotalGrossWages()).isEqualByComparingTo("150000.00");
        assertThat(response.getTotalNetDisbursed()).isEqualByComparingTo("132000.00");
        assertThat(response.getTotalPfContribution()).isEqualByComparingTo("7200.00");
        assertThat(response.getTotalEsicContribution()).isEqualByComparingTo("1500.00");
        assertThat(response.getTotalPtContribution()).isEqualByComparingTo("400.00");
        assertThat(response.getTotalTdsDeducted()).isEqualByComparingTo("12000.00");
        assertThat(response.getEcrFileStatus()).isEqualTo("GENERATED");
        assertThat(response.getBankPayoutStatus()).isEqualTo("GENERATED");
        assertThat(response.getFinalizedAt()).isNotNull();
    }

    @Test
    @DisplayName("CA tax audit returns Section 192 and Section 194J TDS breakdowns")
    void testGetTaxAuditSummary() {
        when(payrollRunRepository.findByCompanyIdAndId(tenantId, payrollRunId))
                .thenReturn(Optional.of(payrollRun));

        List<PayslipLineItem> items = List.of(
                // Regular Employee (Section 192)
                PayslipLineItem.builder()
                        .payrollRunId(payrollRunId)
                        .employeeId(emp1Id)
                        .itemCategory("EARNING")
                        .itemName("Basic Pay")
                        .itemAmount(new BigDecimal("70000.00"))
                        .build(),
                PayslipLineItem.builder()
                        .payrollRunId(payrollRunId)
                        .employeeId(emp1Id)
                        .itemCategory("STATUTORY_DEDUCTION")
                        .itemName("TDS / Income Tax")
                        .itemAmount(new BigDecimal("5000.00"))
                        .build(),
                // Consultant Employee (Section 194J)
                PayslipLineItem.builder()
                        .payrollRunId(payrollRunId)
                        .employeeId(emp2Id)
                        .itemCategory("EARNING")
                        .itemName("Professional Fees")
                        .itemAmount(new BigDecimal("80000.00"))
                        .build(),
                PayslipLineItem.builder()
                        .payrollRunId(payrollRunId)
                        .employeeId(emp2Id)
                        .itemCategory("STATUTORY_DEDUCTION")
                        .itemName("TDS Section 194J")
                        .itemAmount(new BigDecimal("8000.00"))
                        .build()
        );

        when(payslipLineItemRepository.findByCompanyIdAndPayrollRunId(tenantId, payrollRunId))
                .thenReturn(items);
        when(employeeRepository.findByIdAndCompanyId(emp1Id, tenantId))
                .thenReturn(Optional.of(empRegular));
        when(employeeRepository.findByIdAndCompanyId(emp2Id, tenantId))
                .thenReturn(Optional.of(empConsultant));
        when(employeeKycVaultRepository.findByCompanyIdAndEmployeeId(tenantId, emp1Id))
                .thenReturn(Optional.of(kyc1));
        when(employeeKycVaultRepository.findByCompanyIdAndEmployeeId(tenantId, emp2Id))
                .thenReturn(Optional.of(kyc2));

        CaTaxAuditSummaryResponse response = caPortalService.getTaxAuditSummary(payrollRunId);

        assertThat(response).isNotNull();
        assertThat(response.getTotalTdsSection192()).isEqualByComparingTo("5000.00");
        assertThat(response.getTotalTdsSection194J()).isEqualByComparingTo("8000.00");
        assertThat(response.getTotalTdsSection194C()).isEqualByComparingTo("0.00");
        assertThat(response.getTotalTaxDeducted()).isEqualByComparingTo("13000.00");
        assertThat(response.getAuditLineItems()).hasSize(2);

        CaTaxAuditSummaryResponse.TaxAuditLineItemDto item192 = response.getAuditLineItems().stream()
                .filter(i -> "192".equals(i.getSection()))
                .findFirst().orElseThrow();
        assertThat(item192.getEmployeeName()).isEqualTo("Rohan Verma");
        assertThat(item192.getPanNumber()).isEqualTo("ABCDE1111A");
        assertThat(item192.getRegime()).isEqualTo("NEW");
        assertThat(item192.getGrossAmount()).isEqualByComparingTo("70000.00");
        assertThat(item192.getTdsDeducted()).isEqualByComparingTo("5000.00");

        CaTaxAuditSummaryResponse.TaxAuditLineItemDto item194J = response.getAuditLineItems().stream()
                .filter(i -> "194J".equals(i.getSection()))
                .findFirst().orElseThrow();
        assertThat(item194J.getEmployeeName()).isEqualTo("Dr. Mehta");
        assertThat(item194J.getPanNumber()).isEqualTo("ABCDE2222B");
        assertThat(item194J.getRegime()).isEqualTo("OLD");
        assertThat(item194J.getGrossAmount()).isEqualByComparingTo("80000.00");
        assertThat(item194J.getTdsDeducted()).isEqualByComparingTo("8000.00");
    }

    @Test
    @DisplayName("CA PT returns group employees and taxes state-wise")
    void testGetPtReturnSummary() {
        when(payrollRunRepository.findByCompanyIdAndId(tenantId, payrollRunId))
                .thenReturn(Optional.of(payrollRun));

        List<PayslipLineItem> items = List.of(
                PayslipLineItem.builder()
                        .payrollRunId(payrollRunId)
                        .employeeId(emp1Id)
                        .itemCategory("EARNING")
                        .itemName("Basic Pay")
                        .itemAmount(new BigDecimal("50000.00"))
                        .build(),
                PayslipLineItem.builder()
                        .payrollRunId(payrollRunId)
                        .employeeId(emp1Id)
                        .itemCategory("STATUTORY_DEDUCTION")
                        .itemName("Professional Tax")
                        .itemAmount(new BigDecimal("200.00"))
                        .build(),
                PayslipLineItem.builder()
                        .payrollRunId(payrollRunId)
                        .employeeId(emp2Id)
                        .itemCategory("EARNING")
                        .itemName("Basic Pay")
                        .itemAmount(new BigDecimal("60000.00"))
                        .build(),
                PayslipLineItem.builder()
                        .payrollRunId(payrollRunId)
                        .employeeId(emp2Id)
                        .itemCategory("STATUTORY_DEDUCTION")
                        .itemName("Professional Tax")
                        .itemAmount(new BigDecimal("200.00"))
                        .build()
        );

        when(payslipLineItemRepository.findByCompanyIdAndPayrollRunId(tenantId, payrollRunId))
                .thenReturn(items);
        when(employeeRepository.findByIdAndCompanyId(emp1Id, tenantId))
                .thenReturn(Optional.of(empRegular));
        when(employeeRepository.findByIdAndCompanyId(emp2Id, tenantId))
                .thenReturn(Optional.of(empConsultant));
        when(branchRepository.findById(branchKaId))
                .thenReturn(Optional.of(branchKa));
        when(branchRepository.findById(branchMhId))
                .thenReturn(Optional.of(branchMh));

        CaPtReturnSummaryResponse response = caPortalService.getPtReturnSummary(payrollRunId);

        assertThat(response).isNotNull();
        assertThat(response.getTotalPtPayable()).isEqualByComparingTo("400.00");
        assertThat(response.getStateBreakdowns()).hasSize(2);

        CaPtReturnSummaryResponse.StatePtBreakdownDto ka = response.getStateBreakdowns().stream()
                .filter(b -> "KA".equals(b.getStateCode()))
                .findFirst().orElseThrow();
        assertThat(ka.getEmployeeCount()).isEqualTo(1);
        assertThat(ka.getTotalSalary()).isEqualByComparingTo("50000.00");
        assertThat(ka.getTaxAmount()).isEqualByComparingTo("200.00");

        CaPtReturnSummaryResponse.StatePtBreakdownDto mh = response.getStateBreakdowns().stream()
                .filter(b -> "MH".equals(b.getStateCode()))
                .findFirst().orElseThrow();
        assertThat(mh.getEmployeeCount()).isEqualTo(1);
        assertThat(mh.getTotalSalary()).isEqualByComparingTo("60000.00");
        assertThat(mh.getTaxAmount()).isEqualByComparingTo("200.00");
    }

    @Test
    @DisplayName("Download ECR and Bank payout file streams delegate correctly")
    void testDownloadStreams() {
        byte[] mockEcr = "MOCK_ECR_BYTES".getBytes();
        when(ecrExporterService.generateEcrBytes(payrollRunId)).thenReturn(mockEcr);

        byte[] mockBank = "MOCK_BANK_CSV".getBytes();
        when(bankExporterService.generateBankCsvBytes(payrollRunId, "HDFC")).thenReturn(mockBank);

        assertThat(caPortalService.downloadEcrFile(payrollRunId)).isEqualTo(mockEcr);
        assertThat(caPortalService.downloadBankPayoutFile(payrollRunId, "HDFC")).isEqualTo(mockBank);
    }
}

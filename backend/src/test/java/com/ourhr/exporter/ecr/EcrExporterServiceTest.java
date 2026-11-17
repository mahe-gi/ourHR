package com.ourhr.exporter.ecr;

import com.ourhr.attendance.MonthlyAttendanceSummary;
import com.ourhr.attendance.MonthlyAttendanceSummaryRepository;
import com.ourhr.common.TenantContext;
import com.ourhr.company.Company;
import com.ourhr.company.CompanyRepository;
import com.ourhr.employee.Employee;
import com.ourhr.employee.EmployeeRepository;
import com.ourhr.employee.kyc.EmployeeKycVault;
import com.ourhr.employee.kyc.EmployeeKycVaultRepository;
import com.ourhr.employee.salary.EmployeeSalaryStructure;
import com.ourhr.employee.salary.SalaryStructureService;
import com.ourhr.exporter.dto.EcrExportResponse;
import com.ourhr.exporter.storage.FileStorageService;
import com.ourhr.payroll.PayrollRun;
import com.ourhr.payroll.PayrollRunRepository;
import com.ourhr.payroll.PayslipLineItem;
import com.ourhr.payroll.PayslipLineItemRepository;
import com.ourhr.statutory.StatutoryQueryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EcrExporterServiceTest {

    @Mock
    private PayrollRunRepository payrollRunRepository;
    @Mock
    private PayslipLineItemRepository payslipLineItemRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private EmployeeKycVaultRepository employeeKycVaultRepository;
    @Mock
    private MonthlyAttendanceSummaryRepository monthlyAttendanceSummaryRepository;
    @Mock
    private SalaryStructureService salaryStructureService;
    @Mock
    private CompanyRepository companyRepository;
    @Mock
    private StatutoryQueryService statutoryQueryService;
    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private EcrExporterService ecrExporterService;

    private UUID tenantId;
    private UUID payrollRunId;
    private UUID employeeId;
    private PayrollRun payrollRun;
    private Employee employee;
    private EmployeeKycVault kycVault;
    private Company company;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        payrollRunId = UUID.randomUUID();
        employeeId = UUID.randomUUID();

        TenantContext.setTenantId(tenantId);

        company = Company.builder()
                .id(tenantId)
                .legalName("Acme Solutions Private Limited")
                .pfEstablishmentCode("BGM00123456")
                .signatoryName("Rajesh Sharma")
                .signatoryDesignation("Director")
                .build();

        payrollRun = PayrollRun.builder()
                .payrollMonth(11)
                .payrollYear(2026)
                .runStatus("APPROVED")
                .totalGrossPay(new BigDecimal("50000.00"))
                .totalNetPay(new BigDecimal("45000.00"))
                .totalPfLiability(new BigDecimal("3600.00"))
                .build();
        payrollRun.setId(payrollRunId);
        payrollRun.setCompanyId(tenantId);

        employee = Employee.builder()
                .branchId(UUID.randomUUID())
                .employeeCode("EMP001")
                .firstName("Mahesh")
                .lastName("Kumar")
                .dateOfBirth(LocalDate.of(1990, 5, 15))
                .gender("MALE")
                .dateOfJoining(LocalDate.of(2022, 1, 10))
                .employmentStatus("ACTIVE")
                .workerType("REGULAR")
                .department("Engineering")
                .designation("Senior Software Engineer")
                .build();
        employee.setId(employeeId);
        employee.setCompanyId(tenantId);

        kycVault = EmployeeKycVault.builder()
                .employeeId(employeeId)
                .panNumber("ABCDE1234F")
                .maskedAadhaarToken("[Aadhaar Redacted]")
                .uan("100987654321")
                .bankAccountNumber("123456789012")
                .bankIfsc("HDFC0001234")
                .bankName("HDFC Bank")
                .build();
        kycVault.setCompanyId(tenantId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Generate ECR file with strict 25 columns separated by #~#")
    void testGenerateEcrFileSuccessful() {
        when(payrollRunRepository.findByCompanyIdAndId(tenantId, payrollRunId))
                .thenReturn(Optional.of(payrollRun));
        when(companyRepository.findById(tenantId))
                .thenReturn(Optional.of(company));
        when(statutoryQueryService.getEpfWageCeiling(any(LocalDate.class)))
                .thenReturn(new BigDecimal("15000.00"));

        List<PayslipLineItem> items = List.of(
                PayslipLineItem.builder()
                        .payrollRunId(payrollRunId)
                        .employeeId(employeeId)
                        .itemCategory("EARNING")
                        .itemName("Basic Pay")
                        .itemAmount(new BigDecimal("25000.00"))
                        .build(),
                PayslipLineItem.builder()
                        .payrollRunId(payrollRunId)
                        .employeeId(employeeId)
                        .itemCategory("EARNING")
                        .itemName("House Rent Allowance")
                        .itemAmount(new BigDecimal("10000.00"))
                        .build(),
                PayslipLineItem.builder()
                        .payrollRunId(payrollRunId)
                        .employeeId(employeeId)
                        .itemCategory("STATUTORY_DEDUCTION")
                        .itemName("Employee PF Contribution")
                        .itemAmount(new BigDecimal("1800.00"))
                        .build()
        );

        when(payslipLineItemRepository.findByCompanyIdAndPayrollRunId(tenantId, payrollRunId))
                .thenReturn(items);
        when(employeeRepository.findByIdAndCompanyId(employeeId, tenantId))
                .thenReturn(Optional.of(employee));
        when(employeeKycVaultRepository.findByCompanyIdAndEmployeeId(tenantId, employeeId))
                .thenReturn(Optional.of(kycVault));
        when(salaryStructureService.findActiveStructure(eq(employeeId), any(LocalDate.class)))
                .thenReturn(Optional.empty());

        MonthlyAttendanceSummary attendance = MonthlyAttendanceSummary.builder()
                .employeeId(employeeId)
                .payrollMonth(11)
                .payrollYear(2026)
                .totalCalendarDays(30)
                .payableDays(new BigDecimal("28.0"))
                .lopDays(new BigDecimal("2.0"))
                .build();
        when(monthlyAttendanceSummaryRepository.findByCompanyIdAndEmployeeIdAndPayrollMonthAndPayrollYear(
                tenantId, employeeId, 11, 2026))
                .thenReturn(Optional.of(attendance));

        when(fileStorageService.uploadFile(eq("ecr"), anyString(), any(byte[].class), eq("text/plain")))
                .thenReturn("mock/ecr/key.txt");
        when(fileStorageService.generatePresignedUrl(eq("mock/ecr/key.txt"), eq(86400L)))
                .thenReturn("https://storage.ourhr.internal/ecr/ECR_BGM00123456_11_2026.txt");

        EcrExportResponse response = ecrExporterService.generateEcrFile(payrollRunId);

        assertThat(response).isNotNull();
        assertThat(response.getEstablishmentCode()).isEqualTo("BGM00123456");
        assertThat(response.getPayrollMonth()).isEqualTo(11);
        assertThat(response.getPayrollYear()).isEqualTo(2026);
        assertThat(response.getMemberCount()).isEqualTo(1);
        assertThat(response.getTotalGrossWages()).isEqualByComparingTo("35000.00");
        assertThat(response.getTotalEpfWages()).isEqualByComparingTo("15000.00");
        assertThat(response.getTotalEeShare()).isEqualByComparingTo("1800.00");
        assertThat(response.getTotalEpsShare()).isEqualByComparingTo("1250.00");
        assertThat(response.getTotalErShare()).isEqualByComparingTo("550.00");
        assertThat(response.getFileDownloadUrl()).contains("ECR_BGM00123456_11_2026.txt");

        verify(payrollRunRepository).save(payrollRun);
        assertThat(payrollRun.getEcrTxtFileUrl()).isEqualTo("https://storage.ourhr.internal/ecr/ECR_BGM00123456_11_2026.txt");
    }

    @Test
    @DisplayName("Verify exact 25 columns and #~# delimiter syntax in raw text output")
    void testEcrDelimitedSyntax() {
        when(payrollRunRepository.findByCompanyIdAndId(tenantId, payrollRunId))
                .thenReturn(Optional.of(payrollRun));
        when(statutoryQueryService.getEpfWageCeiling(any(LocalDate.class)))
                .thenReturn(new BigDecimal("15000.00"));

        List<PayslipLineItem> items = List.of(
                PayslipLineItem.builder()
                        .payrollRunId(payrollRunId)
                        .employeeId(employeeId)
                        .itemCategory("EARNING")
                        .itemName("Basic Pay")
                        .itemAmount(new BigDecimal("12000.00"))
                        .build()
        );

        when(payslipLineItemRepository.findByCompanyIdAndPayrollRunId(tenantId, payrollRunId))
                .thenReturn(items);
        when(employeeRepository.findByIdAndCompanyId(employeeId, tenantId))
                .thenReturn(Optional.of(employee));
        when(employeeKycVaultRepository.findByCompanyIdAndEmployeeId(tenantId, employeeId))
                .thenReturn(Optional.of(kycVault));

        String rawText = ecrExporterService.generateEcrText(payrollRunId);

        assertThat(rawText).isNotEmpty();
        String[] lines = rawText.split("\n");
        assertThat(lines).hasSize(1);

        String[] columns = lines[0].split(EcrExporterService.DELIMITER, -1);
        assertThat(columns).hasSize(25);

        // Column 1: UAN
        assertThat(columns[0]).isEqualTo("100987654321");
        // Column 2: Member Name
        assertThat(columns[1]).isEqualTo("Mahesh Kumar");
        // Column 3: Gross Wages
        assertThat(columns[2]).isEqualTo("12000.00");
        // Column 4: EPF Wages
        assertThat(columns[3]).isEqualTo("12000.00");
        // Column 5: EPS Wages
        assertThat(columns[4]).isEqualTo("12000.00");
        // Column 6: EDLI Wages
        assertThat(columns[5]).isEqualTo("12000.00");
        // Column 7: EE Share (12% of 12000 = 1440.00)
        assertThat(columns[6]).isEqualTo("1440.00");
        // Column 8: EPS Share (8.33% of 12000 = 999.60)
        assertThat(columns[7]).isEqualTo("999.60");
        // Column 9: ER Share (1440.00 - 999.60 = 440.40)
        assertThat(columns[8]).isEqualTo("440.40");
        // Column 10: NCP Days
        assertThat(columns[9]).isEqualTo("0");
        // Column 11: Total Advances
        assertThat(columns[10]).isEqualTo("0");
        // Columns 12-15: Arrears
        assertThat(columns[11]).isEqualTo("0");
        assertThat(columns[12]).isEqualTo("0");
        assertThat(columns[13]).isEqualTo("0");
        assertThat(columns[14]).isEqualTo("0");
        // Column 16: Father/Husband Name
        assertThat(columns[15]).isEqualTo("-");
        // Column 17: Relationship
        assertThat(columns[16]).isEqualTo("F");
        // Column 18: Date of Birth (dd-MM-yyyy)
        assertThat(columns[17]).isEqualTo("15-05-1990");
        // Column 19: Gender
        assertThat(columns[18]).isEqualTo("M");
        // Column 20: DOJ EPF
        assertThat(columns[19]).isEqualTo("10-01-2022");
        // Column 21: DOJ EPS
        assertThat(columns[20]).isEqualTo("10-01-2022");
        // Column 22: Date of Exit EPF
        assertThat(columns[21]).isEmpty();
        // Column 23: Date of Exit EPS
        assertThat(columns[22]).isEmpty();
        // Column 24: Reason for Exit
        assertThat(columns[23]).isEmpty();
        // Column 25: PMRPY Beneficiary
        assertThat(columns[24]).isEqualTo("N");
    }

    @Test
    @DisplayName("Consultants under Section 194J are excluded from EPFO ECR file")
    void testConsultantsExcludedFromEcr() {
        employee.setWorkerType("CONSULTANT_194J");

        when(payrollRunRepository.findByCompanyIdAndId(tenantId, payrollRunId))
                .thenReturn(Optional.of(payrollRun));

        List<PayslipLineItem> items = List.of(
                PayslipLineItem.builder()
                        .payrollRunId(payrollRunId)
                        .employeeId(employeeId)
                        .itemCategory("EARNING")
                        .itemName("Professional Fees")
                        .itemAmount(new BigDecimal("75000.00"))
                        .build()
        );

        when(payslipLineItemRepository.findByCompanyIdAndPayrollRunId(tenantId, payrollRunId))
                .thenReturn(items);
        when(employeeRepository.findByIdAndCompanyId(employeeId, tenantId))
                .thenReturn(Optional.of(employee));

        String rawText = ecrExporterService.generateEcrText(payrollRunId);

        assertThat(rawText).isEmpty();
    }

    @Test
    @DisplayName("PF opted-out employees are excluded from EPFO ECR file")
    void testPfOptedOutEmployeesExcludedFromEcr() {
        when(payrollRunRepository.findByCompanyIdAndId(tenantId, payrollRunId))
                .thenReturn(Optional.of(payrollRun));

        List<PayslipLineItem> items = List.of(
                PayslipLineItem.builder()
                        .payrollRunId(payrollRunId)
                        .employeeId(employeeId)
                        .itemCategory("EARNING")
                        .itemName("Basic Pay")
                        .itemAmount(new BigDecimal("50000.00"))
                        .build()
        );

        when(payslipLineItemRepository.findByCompanyIdAndPayrollRunId(tenantId, payrollRunId))
                .thenReturn(items);
        when(employeeRepository.findByIdAndCompanyId(employeeId, tenantId))
                .thenReturn(Optional.of(employee));
        when(salaryStructureService.findActiveStructure(eq(employeeId), any(LocalDate.class)))
                .thenReturn(Optional.of(EmployeeSalaryStructure.builder().pfOptOut(true).build()));

        String rawText = ecrExporterService.generateEcrText(payrollRunId);

        assertThat(rawText).isEmpty();
    }

    @Test
    @DisplayName("Contractors under Section 194C are excluded from EPFO ECR file")
    void testContractorsExcludedFromEcr() {
        employee.setWorkerType("CONTRACTOR_194C");

        when(payrollRunRepository.findByCompanyIdAndId(tenantId, payrollRunId))
                .thenReturn(Optional.of(payrollRun));

        List<PayslipLineItem> items = List.of(
                PayslipLineItem.builder()
                        .payrollRunId(payrollRunId)
                        .employeeId(employeeId)
                        .itemCategory("EARNING")
                        .itemName("Contractor Payout")
                        .itemAmount(new BigDecimal("35000.00"))
                        .build()
        );

        when(payslipLineItemRepository.findByCompanyIdAndPayrollRunId(tenantId, payrollRunId))
                .thenReturn(items);
        when(employeeRepository.findByIdAndCompanyId(employeeId, tenantId))
                .thenReturn(Optional.of(employee));

        String rawText = ecrExporterService.generateEcrText(payrollRunId);

        assertThat(rawText).isEmpty();
    }

    @Test
    @DisplayName("NCP days strictly equal attendance LOP days")
    void testNcpDaysMatchesAttendanceLopDays() {
        when(payrollRunRepository.findByCompanyIdAndId(tenantId, payrollRunId))
                .thenReturn(Optional.of(payrollRun));
        when(statutoryQueryService.getEpfWageCeiling(any(LocalDate.class)))
                .thenReturn(new BigDecimal("15000.00"));

        List<PayslipLineItem> items = List.of(
                PayslipLineItem.builder()
                        .payrollRunId(payrollRunId)
                        .employeeId(employeeId)
                        .itemCategory("EARNING")
                        .itemName("Basic Pay")
                        .itemAmount(new BigDecimal("20000.00"))
                        .build()
        );

        MonthlyAttendanceSummary attendance = MonthlyAttendanceSummary.builder()
                .employeeId(employeeId)
                .payrollMonth(11)
                .payrollYear(2026)
                .totalCalendarDays(30)
                .payableDays(new BigDecimal("26.0"))
                .lopDays(new BigDecimal("4.0"))
                .build();

        when(payslipLineItemRepository.findByCompanyIdAndPayrollRunId(tenantId, payrollRunId))
                .thenReturn(items);
        when(employeeRepository.findByIdAndCompanyId(employeeId, tenantId))
                .thenReturn(Optional.of(employee));
        when(employeeKycVaultRepository.findByCompanyIdAndEmployeeId(tenantId, employeeId))
                .thenReturn(Optional.of(kycVault));
        when(monthlyAttendanceSummaryRepository.findByCompanyIdAndEmployeeIdAndPayrollMonthAndPayrollYear(
                tenantId, employeeId, 11, 2026))
                .thenReturn(Optional.of(attendance));

        String rawText = ecrExporterService.generateEcrText(payrollRunId);
        String[] columns = rawText.trim().split(EcrExporterService.DELIMITER, -1);

        assertThat(columns[9]).isEqualTo("4");
    }

    @Test
    @DisplayName("Wage base is capped at INR 15,000.00 with EE 12%, EPS 8.33% capped at 1250.00, and ER difference")
    void testWageBaseCappedAt15000AndSplits() {
        when(payrollRunRepository.findByCompanyIdAndId(tenantId, payrollRunId))
                .thenReturn(Optional.of(payrollRun));
        when(statutoryQueryService.getEpfWageCeiling(any(LocalDate.class)))
                .thenReturn(new BigDecimal("15000.00"));

        List<PayslipLineItem> items = List.of(
                PayslipLineItem.builder()
                        .payrollRunId(payrollRunId)
                        .employeeId(employeeId)
                        .itemCategory("EARNING")
                        .itemName("Basic Pay")
                        .itemAmount(new BigDecimal("60000.00"))
                        .build()
        );

        when(payslipLineItemRepository.findByCompanyIdAndPayrollRunId(tenantId, payrollRunId))
                .thenReturn(items);
        when(employeeRepository.findByIdAndCompanyId(employeeId, tenantId))
                .thenReturn(Optional.of(employee));
        when(employeeKycVaultRepository.findByCompanyIdAndEmployeeId(tenantId, employeeId))
                .thenReturn(Optional.of(kycVault));

        String rawText = ecrExporterService.generateEcrText(payrollRunId);
        String[] columns = rawText.trim().split(EcrExporterService.DELIMITER, -1);

        assertThat(columns[2]).isEqualTo("60000.00"); // Gross
        assertThat(columns[3]).isEqualTo("15000.00"); // EPF Wages capped at 15000
        assertThat(columns[4]).isEqualTo("15000.00"); // EPS Wages capped at 15000
        assertThat(columns[5]).isEqualTo("15000.00"); // EDLI Wages capped at 15000
        assertThat(columns[6]).isEqualTo("1800.00");  // EE Share: 12% of 15000
        assertThat(columns[7]).isEqualTo("1250.00");  // EPS Share: capped at 1250.00
        assertThat(columns[8]).isEqualTo("550.00");   // ER Share: 1800.00 - 1250.00
    }
}

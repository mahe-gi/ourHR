package com.ourhr.exporter.pdf;

import com.ourhr.attendance.MonthlyAttendanceSummary;
import com.ourhr.attendance.MonthlyAttendanceSummaryRepository;
import com.ourhr.common.TenantContext;
import com.ourhr.company.Branch;
import com.ourhr.company.BranchRepository;
import com.ourhr.company.Company;
import com.ourhr.company.CompanyRepository;
import com.ourhr.employee.Employee;
import com.ourhr.employee.EmployeeRepository;
import com.ourhr.employee.kyc.EmployeeKycVault;
import com.ourhr.employee.kyc.EmployeeKycVaultRepository;
import com.ourhr.exporter.dto.PayslipGenerationResponse;
import com.ourhr.exporter.storage.FileStorageService;
import com.ourhr.jobs.BatchJob;
import com.ourhr.jobs.BatchJobService;
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
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PdfPayslipServiceTest {

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
    private CompanyRepository companyRepository;
    @Mock
    private BranchRepository branchRepository;
    @Mock
    private BatchJobService batchJobService;
    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private PdfPayslipService pdfPayslipService;

    private UUID tenantId;
    private UUID payrollRunId;
    private UUID employeeId;
    private UUID branchId;
    private PayrollRun payrollRun;
    private Employee employee;
    private Company company;
    private Branch branch;
    private EmployeeKycVault kycVault;
    private MonthlyAttendanceSummary attendanceSummary;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        payrollRunId = UUID.randomUUID();
        employeeId = UUID.randomUUID();
        branchId = UUID.randomUUID();

        TenantContext.setTenantId(tenantId);

        company = Company.builder()
                .id(tenantId)
                .legalName("Innovatech Labs Private Limited")
                .tradeName("Innovatech")
                .panNumber("ABCDE1234F")
                .tanNumber("BLRI01234E")
                .gstin("29ABCDE1234F1Z5")
                .signatoryName("Ananya Sharma")
                .signatoryDesignation("Managing Director")
                .build();

        branch = Branch.builder()
                .branchName("Bangalore HQ")
                .addressLine("100 Feet Road, Indiranagar")
                .city("Bengaluru")
                .stateCode("KA")
                .pincode("560038")
                .build();
        branch.setId(branchId);
        branch.setCompanyId(tenantId);

        payrollRun = PayrollRun.builder()
                .payrollMonth(11)
                .payrollYear(2026)
                .runStatus("APPROVED")
                .totalGrossPay(new BigDecimal("75000.00"))
                .totalNetPay(new BigDecimal("68000.00"))
                .initiatedBy(UUID.randomUUID())
                .build();
        payrollRun.setId(payrollRunId);
        payrollRun.setCompanyId(tenantId);

        employee = Employee.builder()
                .branchId(branchId)
                .employeeCode("EMP-007")
                .firstName("Arjun")
                .lastName("Verma")
                .dateOfBirth(LocalDate.of(1992, 8, 20))
                .gender("MALE")
                .dateOfJoining(LocalDate.of(2021, 6, 1))
                .department("Engineering")
                .designation("Staff Engineer")
                .employmentStatus("ACTIVE")
                .workerType("REGULAR")
                .build();
        employee.setId(employeeId);
        employee.setCompanyId(tenantId);

        kycVault = EmployeeKycVault.builder()
                .employeeId(employeeId)
                .panNumber("ABCDE9876Z")
                .uan("100234567890")
                .bankAccountNumber("50100234567891")
                .bankIfsc("HDFC0000001")
                .bankName("HDFC Bank")
                .build();
        kycVault.setCompanyId(tenantId);

        attendanceSummary = MonthlyAttendanceSummary.builder()
                .employeeId(employeeId)
                .payrollMonth(11)
                .payrollYear(2026)
                .totalCalendarDays(30)
                .payableDays(new BigDecimal("30.0"))
                .lopDays(BigDecimal.ZERO)
                .build();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Generate structured PDF 1.4 output with header, trailer, and metadata")
    void testGeneratePayslipPdfStructure() {
        when(payrollRunRepository.findByCompanyIdAndId(tenantId, payrollRunId))
                .thenReturn(Optional.of(payrollRun));
        when(employeeRepository.findByIdAndCompanyId(employeeId, tenantId))
                .thenReturn(Optional.of(employee));
        when(companyRepository.findById(tenantId))
                .thenReturn(Optional.of(company));
        when(branchRepository.findById(branchId))
                .thenReturn(Optional.of(branch));
        when(employeeKycVaultRepository.findByCompanyIdAndEmployeeId(tenantId, employeeId))
                .thenReturn(Optional.of(kycVault));
        when(monthlyAttendanceSummaryRepository.findByCompanyIdAndEmployeeIdAndPayrollMonthAndPayrollYear(
                tenantId, employeeId, 11, 2026))
                .thenReturn(Optional.of(attendanceSummary));

        List<PayslipLineItem> items = List.of(
                PayslipLineItem.builder()
                        .payrollRunId(payrollRunId)
                        .employeeId(employeeId)
                        .itemCategory("EARNING")
                        .itemName("Basic Pay")
                        .itemAmount(new BigDecimal("40000.00"))
                        .build(),
                PayslipLineItem.builder()
                        .payrollRunId(payrollRunId)
                        .employeeId(employeeId)
                        .itemCategory("EARNING")
                        .itemName("House Rent Allowance")
                        .itemAmount(new BigDecimal("20000.00"))
                        .build(),
                PayslipLineItem.builder()
                        .payrollRunId(payrollRunId)
                        .employeeId(employeeId)
                        .itemCategory("STATUTORY_DEDUCTION")
                        .itemName("Employee PF Contribution")
                        .itemAmount(new BigDecimal("1800.00"))
                        .build(),
                PayslipLineItem.builder()
                        .payrollRunId(payrollRunId)
                        .employeeId(employeeId)
                        .itemCategory("STATUTORY_DEDUCTION")
                        .itemName("Professional Tax")
                        .itemAmount(new BigDecimal("200.00"))
                        .build()
        );

        when(payslipLineItemRepository.findByCompanyIdAndPayrollRunIdAndEmployeeId(tenantId, payrollRunId, employeeId))
                .thenReturn(items);

        byte[] pdfBytes = pdfPayslipService.generatePayslipPdf(payrollRunId, employeeId);

        assertThat(pdfBytes).isNotEmpty();
        String pdfContent = new String(pdfBytes, StandardCharsets.ISO_8859_1);

        // Header check
        assertThat(pdfContent).startsWith("%PDF-1.4");
        // Trailer check
        assertThat(pdfContent).contains("%%EOF");
        assertThat(pdfContent.trim()).endsWith("%%EOF");
        // Company & Employee metadata
        assertThat(pdfContent).contains("Innovatech Labs Private Limited");
        assertThat(pdfContent).contains("EMP-007");
        assertThat(pdfContent).contains("Arjun Verma");
        assertThat(pdfContent).contains("Staff Engineer");
        // Masked bank account
        assertThat(pdfContent).contains("******7891");
        assertThat(pdfContent).doesNotContain("50100234567891");
        // Net pay in numbers and words (60000 - 2000 = 58000.00)
        assertThat(pdfContent).contains("58000.00");
        assertThat(pdfContent).contains("Rupees Fifty Eight Thousand Only");
    }

    @Test
    @DisplayName("Render and store payslip PDF updates payslipPdfUrl on PayslipLineItem")
    void testRenderAndStorePayslip() {
        when(payrollRunRepository.findByCompanyIdAndId(tenantId, payrollRunId))
                .thenReturn(Optional.of(payrollRun));
        when(employeeRepository.findByIdAndCompanyId(employeeId, tenantId))
                .thenReturn(Optional.of(employee));
        when(companyRepository.findById(tenantId))
                .thenReturn(Optional.of(company));
        when(branchRepository.findById(branchId))
                .thenReturn(Optional.of(branch));
        when(employeeKycVaultRepository.findByCompanyIdAndEmployeeId(tenantId, employeeId))
                .thenReturn(Optional.of(kycVault));
        when(monthlyAttendanceSummaryRepository.findByCompanyIdAndEmployeeIdAndPayrollMonthAndPayrollYear(
                tenantId, employeeId, 11, 2026))
                .thenReturn(Optional.of(attendanceSummary));

        PayslipLineItem lineItem = PayslipLineItem.builder()
                .payrollRunId(payrollRunId)
                .employeeId(employeeId)
                .itemCategory("EARNING")
                .itemName("Basic Pay")
                .itemAmount(new BigDecimal("50000.00"))
                .build();
        lineItem.setCompanyId(tenantId);

        when(payslipLineItemRepository.findByCompanyIdAndPayrollRunIdAndEmployeeId(tenantId, payrollRunId, employeeId))
                .thenReturn(List.of(lineItem));

        when(fileStorageService.uploadFile(eq("payslips"), anyString(), any(byte[].class), eq("application/pdf")))
                .thenReturn("mock/payslip/key.pdf");
        when(fileStorageService.generatePresignedUrl(eq("mock/payslip/key.pdf"), eq(86400L)))
                .thenReturn("https://storage.ourhr.internal/payslips/EMP-007_11_2026.pdf");

        String downloadUrl = pdfPayslipService.renderAndStorePayslip(payrollRunId, employeeId);

        assertThat(downloadUrl).isEqualTo("https://storage.ourhr.internal/payslips/EMP-007_11_2026.pdf");
        assertThat(lineItem.getPayslipPdfUrl()).isEqualTo("https://storage.ourhr.internal/payslips/EMP-007_11_2026.pdf");
        verify(payslipLineItemRepository).saveAll(any());
    }

    @Test
    @DisplayName("Schedule async batch job for PDF payslip generation")
    void testGeneratePayslipsAsync() {
        when(payrollRunRepository.findByCompanyIdAndId(tenantId, payrollRunId))
                .thenReturn(Optional.of(payrollRun));

        PayslipLineItem item1 = PayslipLineItem.builder()
                .payrollRunId(payrollRunId)
                .employeeId(employeeId)
                .itemCategory("EARNING")
                .itemName("Basic Pay")
                .itemAmount(new BigDecimal("50000.00"))
                .build();

        when(payslipLineItemRepository.findByCompanyIdAndPayrollRunId(tenantId, payrollRunId))
                .thenReturn(List.of(item1));

        UUID jobId = UUID.randomUUID();
        BatchJob mockJob = BatchJob.builder()
                .jobType("PAYSLIP_PDF_GENERATION")
                .status("PENDING")
                .totalItems(1)
                .build();
        mockJob.setId(jobId);

        when(batchJobService.createJob(eq(tenantId), eq("PAYSLIP_PDF_GENERATION"), any(UUID.class), eq(1), anyString()))
                .thenReturn(mockJob);

        PayslipGenerationResponse response = pdfPayslipService.generatePayslipsAsync(payrollRunId);

        assertThat(response).isNotNull();
        assertThat(response.getBatchJobId()).isEqualTo(jobId);
        assertThat(response.getPayrollRunId()).isEqualTo(payrollRunId);
        assertThat(response.getStatus()).isEqualTo("PENDING");
        assertThat(response.getTotalPayslipsScheduled()).isEqualTo(1);
    }
}

package com.ourhr.exporter.bank;

import com.ourhr.common.TenantContext;
import com.ourhr.employee.Employee;
import com.ourhr.employee.EmployeeRepository;
import com.ourhr.employee.kyc.EmployeeKycVault;
import com.ourhr.employee.kyc.EmployeeKycVaultRepository;
import com.ourhr.exporter.dto.BankExportRequest;
import com.ourhr.exporter.dto.BankExportResponse;
import com.ourhr.exporter.storage.FileStorageService;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BankExporterServiceTest {

    @Mock
    private PayrollRunRepository payrollRunRepository;
    @Mock
    private PayslipLineItemRepository payslipLineItemRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private EmployeeKycVaultRepository employeeKycVaultRepository;
    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private BankExporterService bankExporterService;

    private UUID tenantId;
    private UUID payrollRunId;
    private UUID employeeId;
    private PayrollRun payrollRun;
    private Employee employee;
    private EmployeeKycVault kycVault;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        payrollRunId = UUID.randomUUID();
        employeeId = UUID.randomUUID();

        TenantContext.setTenantId(tenantId);

        payrollRun = PayrollRun.builder()
                .payrollMonth(11)
                .payrollYear(2026)
                .runStatus("APPROVED")
                .totalGrossPay(new BigDecimal("60000.00"))
                .totalNetPay(new BigDecimal("55000.00"))
                .build();
        payrollRun.setId(payrollRunId);
        payrollRun.setCompanyId(tenantId);

        employee = Employee.builder()
                .employeeCode("EMP-101")
                .firstName("Suresh")
                .lastName("Raina")
                .employmentStatus("ACTIVE")
                .workerType("REGULAR")
                .build();
        employee.setId(employeeId);
        employee.setCompanyId(tenantId);

        kycVault = EmployeeKycVault.builder()
                .employeeId(employeeId)
                .bankAccountNumber("987654321098")
                .bankIfsc("HDFC0004321")
                .bankName("HDFC Bank")
                .build();
        kycVault.setCompanyId(tenantId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Export HDFC CSV format matches expected headers and columns")
    void testExportHdfcFormat() {
        when(payrollRunRepository.findByCompanyIdAndId(tenantId, payrollRunId))
                .thenReturn(Optional.of(payrollRun));

        List<PayslipLineItem> items = List.of(
                PayslipLineItem.builder()
                        .payrollRunId(payrollRunId)
                        .employeeId(employeeId)
                        .itemCategory("EARNING")
                        .itemName("Basic Pay")
                        .itemAmount(new BigDecimal("50000.00"))
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

        when(fileStorageService.uploadFile(eq("bank-payouts"), anyString(), any(byte[].class), eq("text/csv")))
                .thenReturn("mock/bank/hdfc.csv");
        when(fileStorageService.generatePresignedUrl(eq("mock/bank/hdfc.csv"), eq(86400L)))
                .thenReturn("https://storage.ourhr.internal/bank/hdfc.csv");

        BankExportRequest request = BankExportRequest.builder()
                .payrollRunId(payrollRunId)
                .bankFormat("HDFC")
                .build();

        BankExportResponse response = bankExporterService.exportBankPayout(request);

        assertThat(response).isNotNull();
        assertThat(response.getBankFormat()).isEqualTo("HDFC");
        assertThat(response.getTotalRecords()).isEqualTo(1);
        assertThat(response.getTotalAmount()).isEqualByComparingTo("48200.00");
        assertThat(response.getFileDownloadUrl()).isEqualTo("https://storage.ourhr.internal/bank/hdfc.csv");

        verify(payrollRunRepository).save(payrollRun);
        assertThat(payrollRun.getBankPayoutFileUrl()).isEqualTo("https://storage.ourhr.internal/bank/hdfc.csv");
    }

    @Test
    @DisplayName("Verify CSV raw text output across HDFC, ICICI, SBI, and AXIS bank formats")
    void testBankFormatsCsvOutput() {
        when(payrollRunRepository.findByCompanyIdAndId(tenantId, payrollRunId))
                .thenReturn(Optional.of(payrollRun));

        List<PayslipLineItem> items = List.of(
                PayslipLineItem.builder()
                        .payrollRunId(payrollRunId)
                        .employeeId(employeeId)
                        .itemCategory("EARNING")
                        .itemName("Basic Pay")
                        .itemAmount(new BigDecimal("30000.00"))
                        .build()
        );

        when(payslipLineItemRepository.findByCompanyIdAndPayrollRunId(tenantId, payrollRunId))
                .thenReturn(items);
        when(employeeRepository.findByIdAndCompanyId(employeeId, tenantId))
                .thenReturn(Optional.of(employee));
        when(employeeKycVaultRepository.findByCompanyIdAndEmployeeId(tenantId, employeeId))
                .thenReturn(Optional.of(kycVault));

        // 1. HDFC
        String hdfcCsv = bankExporterService.generateBankCsv(payrollRunId, "HDFC");
        assertThat(hdfcCsv).startsWith("Beneficiary Account No,Beneficiary Name,Payment Amount,Payment Date,IFSC Code,Remarks\n");
        assertThat(hdfcCsv).contains("987654321098,Suresh Raina,30000.00,");
        assertThat(hdfcCsv).contains(",HDFC0004321,Salary 11/2026\n");

        // 2. ICICI
        String iciciCsv = bankExporterService.generateBankCsv(payrollRunId, "ICICI");
        assertThat(iciciCsv).startsWith("Debit Account No,Beneficiary Account No,Beneficiary Name,Amount,Currency,Payment Date,IFSC Code\n");
        assertThat(iciciCsv).contains("CORPORATE_ACCOUNT,987654321098,Suresh Raina,30000.00,INR,");

        // 3. SBI
        String sbiCsv = bankExporterService.generateBankCsv(payrollRunId, "SBI");
        assertThat(sbiCsv).startsWith("Account Number,Amount,Beneficiary Name,IFSC,Narration\n");
        assertThat(sbiCsv).contains("987654321098,30000.00,Suresh Raina,HDFC0004321,Salary 11/2026");

        // 4. AXIS
        String axisCsv = bankExporterService.generateBankCsv(payrollRunId, "AXIS");
        assertThat(axisCsv).startsWith("Bene Account No,Amount,Beneficiary Name,IFSC,Transaction Reference\n");
        assertThat(axisCsv).contains("987654321098,30000.00,Suresh Raina,HDFC0004321,SAL112026-EMP-101");
    }

    @Test
    @DisplayName("Validate multiple beneficiary accounts, IFSC codes, and total payout matching net pay")
    void testMultipleBeneficiariesTotalMatchesPayrollNetPay() {
        UUID employeeId2 = UUID.randomUUID();
        Employee employee2 = Employee.builder()
                .employeeCode("EMP-102")
                .firstName("Virat")
                .lastName("Kohli")
                .employmentStatus("ACTIVE")
                .workerType("REGULAR")
                .build();
        employee2.setId(employeeId2);
        employee2.setCompanyId(tenantId);

        EmployeeKycVault kycVault2 = EmployeeKycVault.builder()
                .employeeId(employeeId2)
                .bankAccountNumber("112233445566")
                .bankIfsc("ICIC0001122")
                .bankName("ICICI Bank")
                .build();
        kycVault2.setCompanyId(tenantId);

        // Employee 1: 50,000 earning - 2,000 deduction = 48,000 net
        // Employee 2: 70,000 earning - 5,000 deduction = 65,000 net
        // Total net = 113,000.00
        List<PayslipLineItem> items = List.of(
                PayslipLineItem.builder()
                        .payrollRunId(payrollRunId)
                        .employeeId(employeeId)
                        .itemCategory("EARNING")
                        .itemName("Basic Pay")
                        .itemAmount(new BigDecimal("50000.00"))
                        .build(),
                PayslipLineItem.builder()
                        .payrollRunId(payrollRunId)
                        .employeeId(employeeId)
                        .itemCategory("DEDUCTION")
                        .itemName("TDS")
                        .itemAmount(new BigDecimal("2000.00"))
                        .build(),
                PayslipLineItem.builder()
                        .payrollRunId(payrollRunId)
                        .employeeId(employeeId2)
                        .itemCategory("EARNING")
                        .itemName("Basic Pay")
                        .itemAmount(new BigDecimal("70000.00"))
                        .build(),
                PayslipLineItem.builder()
                        .payrollRunId(payrollRunId)
                        .employeeId(employeeId2)
                        .itemCategory("DEDUCTION")
                        .itemName("TDS")
                        .itemAmount(new BigDecimal("5000.00"))
                        .build()
        );

        when(payrollRunRepository.findByCompanyIdAndId(tenantId, payrollRunId))
                .thenReturn(Optional.of(payrollRun));
        when(payslipLineItemRepository.findByCompanyIdAndPayrollRunId(tenantId, payrollRunId))
                .thenReturn(items);
        when(employeeRepository.findByIdAndCompanyId(employeeId, tenantId))
                .thenReturn(Optional.of(employee));
        when(employeeRepository.findByIdAndCompanyId(employeeId2, tenantId))
                .thenReturn(Optional.of(employee2));
        when(employeeKycVaultRepository.findByCompanyIdAndEmployeeId(tenantId, employeeId))
                .thenReturn(Optional.of(kycVault));
        when(employeeKycVaultRepository.findByCompanyIdAndEmployeeId(tenantId, employeeId2))
                .thenReturn(Optional.of(kycVault2));

        when(fileStorageService.uploadFile(eq("bank-payouts"), anyString(), any(byte[].class), eq("text/csv")))
                .thenReturn("mock/bank/payout.csv");
        when(fileStorageService.generatePresignedUrl(eq("mock/bank/payout.csv"), eq(86400L)))
                .thenReturn("https://storage/bank/payout.csv");

        BankExportResponse response = bankExporterService.exportBankPayout(payrollRunId, "SBI");

        assertThat(response).isNotNull();
        assertThat(response.getTotalRecords()).isEqualTo(2);
        assertThat(response.getTotalAmount()).isEqualByComparingTo("113000.00");

        String sbiCsv = bankExporterService.generateBankCsv(payrollRunId, "SBI");
        // Verify both employee accounts, beneficiary names, and IFSCs are in CSV
        assertThat(sbiCsv).contains("987654321098,48000.00,Suresh Raina,HDFC0004321");
        assertThat(sbiCsv).contains("112233445566,65000.00,Virat Kohli,ICIC0001122");
    }

    @Test
    @DisplayName("Reject unsupported bank formats with IllegalArgumentException")
    void testUnsupportedBankFormatThrows() {
        assertThatThrownBy(() -> bankExporterService.exportBankPayout(payrollRunId, "CITIBANK"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported bank format");

        assertThatThrownBy(() -> bankExporterService.exportBankPayout(payrollRunId, ""))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

package com.ourhr.exporter.bank;

import com.ourhr.common.ResourceNotFoundException;
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
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Generates bank-specific NEFT/RTGS CSV payout disbursement files for processed payroll runs.
 * Supported bank formats: HDFC, ICICI, SBI, and AXIS.
 */
@Service
@RequiredArgsConstructor
public class BankExporterService {

    private static final Logger log = LoggerFactory.getLogger(BankExporterService.class);

    public static final String FORMAT_HDFC = "HDFC";
    public static final String FORMAT_ICICI = "ICICI";
    public static final String FORMAT_SBI = "SBI";
    public static final String FORMAT_AXIS = "AXIS";

    public static final Set<String> SUPPORTED_FORMATS = Set.of(FORMAT_HDFC, FORMAT_ICICI, FORMAT_SBI, FORMAT_AXIS);

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final PayrollRunRepository payrollRunRepository;
    private final PayslipLineItemRepository payslipLineItemRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeKycVaultRepository employeeKycVaultRepository;
    private final FileStorageService fileStorageService;

    @Transactional
    public BankExportResponse exportBankPayout(BankExportRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("BankExportRequest cannot be null");
        }
        return exportBankPayout(request.getPayrollRunId(), request.getBankFormat());
    }

    @Transactional
    public BankExportResponse exportBankPayout(UUID payrollRunId, String bankFormat) {
        String normalizedFormat = normalizeFormat(bankFormat);
        UUID companyId = resolveCurrentCompanyId();
        PayrollRun run = getPayrollRunOrThrow(companyId, payrollRunId);

        List<BankPayoutRow> rows = buildPayoutRows(companyId, run);

        String csvContent = renderCsv(normalizedFormat, rows, run);
        byte[] csvBytes = csvContent.getBytes(StandardCharsets.UTF_8);

        String fileName = String.format(Locale.ROOT, "bank_payout_%s_%02d_%04d.csv",
                normalizedFormat.toLowerCase(Locale.ROOT), run.getPayrollMonth(), run.getPayrollYear());

        String fileKey = fileStorageService.uploadFile("bank-payouts", fileName, csvBytes, "text/csv");
        String downloadUrl = fileStorageService.generatePresignedUrl(fileKey, 86400L);

        run.setBankPayoutFileUrl(downloadUrl);
        payrollRunRepository.save(run);

        BigDecimal totalAmount = rows.stream()
                .map(r -> r.netAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        log.info("Generated {} bank payout CSV for run ID: {}, records: {}, total amount: {}",
                normalizedFormat, payrollRunId, rows.size(), totalAmount);

        return BankExportResponse.builder()
                .payrollRunId(payrollRunId)
                .bankFormat(normalizedFormat)
                .fileName(fileName)
                .fileDownloadUrl(downloadUrl)
                .totalRecords(rows.size())
                .totalAmount(totalAmount)
                .generatedAt(Instant.now())
                .build();
    }

    @Transactional(readOnly = true)
    public byte[] generateBankCsvBytes(UUID payrollRunId, String bankFormat) {
        return generateBankCsv(payrollRunId, bankFormat).getBytes(StandardCharsets.UTF_8);
    }

    @Transactional(readOnly = true)
    public String generateBankCsv(UUID payrollRunId, String bankFormat) {
        String normalizedFormat = normalizeFormat(bankFormat);
        UUID companyId = resolveCurrentCompanyId();
        PayrollRun run = getPayrollRunOrThrow(companyId, payrollRunId);
        List<BankPayoutRow> rows = buildPayoutRows(companyId, run);
        return renderCsv(normalizedFormat, rows, run);
    }

    private List<BankPayoutRow> buildPayoutRows(UUID companyId, PayrollRun run) {
        List<PayslipLineItem> items = payslipLineItemRepository
                .findByCompanyIdAndPayrollRunId(companyId, run.getId());

        Map<UUID, List<PayslipLineItem>> itemsByEmployee = items.stream()
                .collect(Collectors.groupingBy(PayslipLineItem::getEmployeeId));

        List<BankPayoutRow> rows = new ArrayList<>();

        for (Map.Entry<UUID, List<PayslipLineItem>> entry : itemsByEmployee.entrySet()) {
            UUID employeeId = entry.getKey();
            List<PayslipLineItem> empItems = entry.getValue();

            Optional<Employee> empOpt = employeeRepository.findByIdAndCompanyId(employeeId, companyId);
            if (empOpt.isEmpty()) {
                continue;
            }
            Employee emp = empOpt.get();

            BigDecimal gross = empItems.stream()
                    .filter(i -> "EARNING".equalsIgnoreCase(i.getItemCategory()))
                    .map(PayslipLineItem::getItemAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);

            BigDecimal deductions = empItems.stream()
                    .filter(i -> !"EARNING".equalsIgnoreCase(i.getItemCategory()))
                    .map(PayslipLineItem::getItemAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);

            BigDecimal netPay = gross.subtract(deductions).setScale(2, RoundingMode.HALF_UP);
            if (netPay.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            Optional<EmployeeKycVault> vaultOpt =
                    employeeKycVaultRepository.findByCompanyIdAndEmployeeId(companyId, employeeId);

            String accountNumber = vaultOpt.map(EmployeeKycVault::getBankAccountNumber)
                    .filter(acc -> acc != null && !acc.isBlank()).orElse("");
            String ifsc = vaultOpt.map(EmployeeKycVault::getBankIfsc)
                    .filter(code -> code != null && !code.isBlank()).orElse("");

            String firstName = emp.getFirstName() != null ? emp.getFirstName().trim() : "";
            String lastName = emp.getLastName() != null ? emp.getLastName().trim() : "";
            String memberName = (firstName + (lastName.isBlank() ? "" : " " + lastName)).trim();

            rows.add(new BankPayoutRow(
                    emp.getId(),
                    emp.getEmployeeCode(),
                    memberName,
                    accountNumber,
                    ifsc,
                    netPay
            ));
        }

        return rows;
    }

    private String renderCsv(String bankFormat, List<BankPayoutRow> rows, PayrollRun run) {
        StringBuilder sb = new StringBuilder();
        String paymentDate = LocalDate.now().format(DATE_FORMAT);
        String defaultRemarks = "Salary " + run.getPayrollMonth() + "/" + run.getPayrollYear();

        switch (bankFormat) {
            case FORMAT_HDFC:
                sb.append("Beneficiary Account No,Beneficiary Name,Payment Amount,Payment Date,IFSC Code,Remarks\n");
                for (BankPayoutRow r : rows) {
                    sb.append(escapeCsv(r.accountNumber)).append(",")
                            .append(escapeCsv(r.beneficiaryName)).append(",")
                            .append(r.netAmount.setScale(2, RoundingMode.HALF_UP).toPlainString()).append(",")
                            .append(paymentDate).append(",")
                            .append(escapeCsv(r.ifsc)).append(",")
                            .append(escapeCsv(defaultRemarks)).append("\n");
                }
                break;

            case FORMAT_ICICI:
                sb.append("Debit Account No,Beneficiary Account No,Beneficiary Name,Amount,Currency,Payment Date,IFSC Code\n");
                for (BankPayoutRow r : rows) {
                    sb.append("CORPORATE_ACCOUNT").append(",")
                            .append(escapeCsv(r.accountNumber)).append(",")
                            .append(escapeCsv(r.beneficiaryName)).append(",")
                            .append(r.netAmount.setScale(2, RoundingMode.HALF_UP).toPlainString()).append(",")
                            .append("INR").append(",")
                            .append(paymentDate).append(",")
                            .append(escapeCsv(r.ifsc)).append("\n");
                }
                break;

            case FORMAT_SBI:
                sb.append("Account Number,Amount,Beneficiary Name,IFSC,Narration\n");
                for (BankPayoutRow r : rows) {
                    sb.append(escapeCsv(r.accountNumber)).append(",")
                            .append(r.netAmount.setScale(2, RoundingMode.HALF_UP).toPlainString()).append(",")
                            .append(escapeCsv(r.beneficiaryName)).append(",")
                            .append(escapeCsv(r.ifsc)).append(",")
                            .append(escapeCsv(defaultRemarks)).append("\n");
                }
                break;

            case FORMAT_AXIS:
                sb.append("Bene Account No,Amount,Beneficiary Name,IFSC,Transaction Reference\n");
                for (BankPayoutRow r : rows) {
                    String txRef = String.format(Locale.ROOT, "SAL%02d%04d-%s",
                            run.getPayrollMonth(), run.getPayrollYear(), r.employeeCode != null ? r.employeeCode : "EMP");
                    sb.append(escapeCsv(r.accountNumber)).append(",")
                            .append(r.netAmount.setScale(2, RoundingMode.HALF_UP).toPlainString()).append(",")
                            .append(escapeCsv(r.beneficiaryName)).append(",")
                            .append(escapeCsv(r.ifsc)).append(",")
                            .append(escapeCsv(txRef)).append("\n");
                }
                break;

            default:
                throw new IllegalArgumentException("Unsupported bank format: " + bankFormat);
        }

        return sb.toString();
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.contains(",") || trimmed.contains("\"") || trimmed.contains("\n") || trimmed.contains("\r")) {
            return "\"" + trimmed.replace("\"", "\"\"") + "\"";
        }
        return trimmed;
    }

    private String normalizeFormat(String bankFormat) {
        if (bankFormat == null || bankFormat.isBlank()) {
            throw new IllegalArgumentException("Bank format cannot be empty. Supported formats: HDFC, ICICI, SBI, AXIS");
        }
        String upper = bankFormat.trim().toUpperCase(Locale.ROOT);
        if (!SUPPORTED_FORMATS.contains(upper)) {
            throw new IllegalArgumentException("Unsupported bank format: " + bankFormat + ". Supported formats: HDFC, ICICI, SBI, AXIS");
        }
        return upper;
    }

    private PayrollRun getPayrollRunOrThrow(UUID companyId, UUID payrollRunId) {
        return payrollRunRepository.findByCompanyIdAndId(companyId, payrollRunId)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll run not found with ID: " + payrollRunId));
    }

    private UUID resolveCurrentCompanyId() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant context is required");
        }
        return tenantId;
    }

    public static class BankPayoutRow {
        public final UUID employeeId;
        public final String employeeCode;
        public final String beneficiaryName;
        public final String accountNumber;
        public final String ifsc;
        public final BigDecimal netAmount;

        public BankPayoutRow(UUID employeeId, String employeeCode, String beneficiaryName,
                             String accountNumber, String ifsc, BigDecimal netAmount) {
            this.employeeId = employeeId;
            this.employeeCode = employeeCode;
            this.beneficiaryName = beneficiaryName;
            this.accountNumber = accountNumber;
            this.ifsc = ifsc;
            this.netAmount = netAmount;
        }
    }
}

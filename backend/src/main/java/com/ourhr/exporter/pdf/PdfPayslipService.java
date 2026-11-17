package com.ourhr.exporter.pdf;

import com.ourhr.attendance.MonthlyAttendanceSummary;
import com.ourhr.attendance.MonthlyAttendanceSummaryRepository;
import com.ourhr.common.ResourceNotFoundException;
import com.ourhr.common.TenantContext;
import com.ourhr.company.Branch;
import com.ourhr.company.BranchRepository;
import com.ourhr.company.Company;
import com.ourhr.company.CompanyRepository;
import com.ourhr.employee.Employee;
import com.ourhr.employee.EmployeeRepository;
import com.ourhr.employee.kyc.EmployeeKycVault;
import com.ourhr.employee.kyc.EmployeeKycVaultRepository;
import com.ourhr.employee.kyc.KycVaultService;
import com.ourhr.exporter.dto.PayslipGenerationResponse;
import com.ourhr.exporter.storage.FileStorageService;
import com.ourhr.jobs.BatchJob;
import com.ourhr.jobs.BatchJobService;
import com.ourhr.payroll.PayrollRun;
import com.ourhr.payroll.PayrollRunRepository;
import com.ourhr.payroll.PayslipLineItem;
import com.ourhr.payroll.PayslipLineItemRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Asynchronous PDF payslip generator backed by BatchJobService.
 * Renders company branding, employee details, earnings, statutory deductions,
 * and net salary in numbers and words using structured pure Java PDF 1.4 output.
 */
@Service
@RequiredArgsConstructor
public class PdfPayslipService {

    private static final Logger log = LoggerFactory.getLogger(PdfPayslipService.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private final PayrollRunRepository payrollRunRepository;
    private final PayslipLineItemRepository payslipLineItemRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeKycVaultRepository employeeKycVaultRepository;
    private final MonthlyAttendanceSummaryRepository monthlyAttendanceSummaryRepository;
    private final CompanyRepository companyRepository;
    private final BranchRepository branchRepository;
    private final BatchJobService batchJobService;
    private final FileStorageService fileStorageService;

    @Transactional
    public String renderAndStorePayslip(UUID payrollRunId, UUID employeeId) {
        UUID companyId = resolveCurrentCompanyId();
        byte[] pdfBytes = generatePayslipPdf(payrollRunId, employeeId);

        Employee emp = employeeRepository.findByIdAndCompanyId(employeeId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with ID: " + employeeId));
        PayrollRun run = getPayrollRunOrThrow(companyId, payrollRunId);

        String fileName = String.format(Locale.ROOT, "payslip_%s_%02d_%04d.pdf",
                emp.getEmployeeCode() != null ? emp.getEmployeeCode() : "EMP",
                run.getPayrollMonth(), run.getPayrollYear());

        String fileKey = fileStorageService.uploadFile("payslips", fileName, pdfBytes, "application/pdf");
        String downloadUrl = fileStorageService.generatePresignedUrl(fileKey, 86400L);

        List<PayslipLineItem> items = payslipLineItemRepository
                .findByCompanyIdAndPayrollRunIdAndEmployeeId(companyId, payrollRunId, employeeId);
        for (PayslipLineItem item : items) {
            item.setPayslipPdfUrl(downloadUrl);
        }
        payslipLineItemRepository.saveAll(items);

        log.info("Rendered and stored PDF payslip for employee ID: {}, run ID: {}", employeeId, payrollRunId);
        return downloadUrl;
    }

    @Transactional(readOnly = true)
    public byte[] generatePayslipPdf(UUID payrollRunId, UUID employeeId) {
        UUID companyId = resolveCurrentCompanyId();
        PayrollRun run = getPayrollRunOrThrow(companyId, payrollRunId);

        Employee employee = employeeRepository.findByIdAndCompanyId(employeeId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with ID: " + employeeId));

        Company company = companyRepository.findById(companyId).orElse(null);
        Branch branch = employee.getBranchId() != null
                ? branchRepository.findById(employee.getBranchId()).orElse(null)
                : null;

        Optional<EmployeeKycVault> vaultOpt =
                employeeKycVaultRepository.findByCompanyIdAndEmployeeId(companyId, employeeId);

        Optional<MonthlyAttendanceSummary> attendanceOpt =
                monthlyAttendanceSummaryRepository.findByCompanyIdAndEmployeeIdAndPayrollMonthAndPayrollYear(
                        companyId, employeeId, run.getPayrollMonth(), run.getPayrollYear());

        List<PayslipLineItem> items = payslipLineItemRepository
                .findByCompanyIdAndPayrollRunIdAndEmployeeId(companyId, payrollRunId, employeeId);

        return buildPdf(company, branch, employee, vaultOpt.orElse(null), attendanceOpt.orElse(null), run, items);
    }

    @Transactional
    public PayslipGenerationResponse generatePayslipsAsync(UUID payrollRunId) {
        UUID companyId = resolveCurrentCompanyId();
        PayrollRun run = getPayrollRunOrThrow(companyId, payrollRunId);

        List<PayslipLineItem> items = payslipLineItemRepository
                .findByCompanyIdAndPayrollRunId(companyId, run.getId());

        List<UUID> employeeIds = items.stream()
                .map(PayslipLineItem::getEmployeeId)
                .distinct()
                .toList();

        UUID initiatorId = run.getInitiatedBy() != null ? run.getInitiatedBy() : UUID.randomUUID();
        String payload = String.format(Locale.ROOT, "{\"payrollRunId\":\"%s\",\"totalEmployees\":%d}",
                payrollRunId, employeeIds.size());

        BatchJob job = batchJobService.createJob(
                companyId,
                "PAYSLIP_PDF_GENERATION",
                initiatorId,
                employeeIds.size(),
                payload
        );

        CompletableFuture.runAsync(() -> {
            try {
                TenantContext.setTenantId(companyId);
                int processed = 0;
                int failed = 0;

                for (UUID empId : employeeIds) {
                    try {
                        renderAndStorePayslip(payrollRunId, empId);
                        processed++;
                    } catch (Exception ex) {
                        log.error("Failed to render payslip for employee ID: {} in run: {}", empId, payrollRunId, ex);
                        failed++;
                    }
                    batchJobService.updateProgress(job.getId(), processed, failed);
                }

                if (failed == 0) {
                    batchJobService.completeJob(job.getId());
                } else if (processed > 0) {
                    batchJobService.updateProgress(job.getId(), processed, failed);
                    batchJobService.completeJob(job.getId());
                } else {
                    batchJobService.failJob(job.getId(), "All payslip renders failed");
                }
            } catch (Exception ex) {
                log.error("Batch payslip generation job failed for run ID: {}", payrollRunId, ex);
                batchJobService.failJob(job.getId(), ex.getMessage());
            } finally {
                TenantContext.clear();
            }
        });

        return PayslipGenerationResponse.builder()
                .batchJobId(job.getId())
                .payrollRunId(payrollRunId)
                .status("PENDING")
                .totalPayslipsScheduled(employeeIds.size())
                .scheduledAt(Instant.now())
                .build();
    }

    private byte[] buildPdf(Company company,
                            Branch branch,
                            Employee emp,
                            EmployeeKycVault vault,
                            MonthlyAttendanceSummary attendance,
                            PayrollRun run,
                            List<PayslipLineItem> items) {

        String companyLegalName = (company != null && company.getLegalName() != null)
                ? company.getLegalName() : "ourHR Enterprise";
        String companyTradeName = (company != null && company.getTradeName() != null)
                ? company.getTradeName() : "";
        String gstin = (company != null && company.getGstin() != null) ? company.getGstin() : "-";
        String tan = (company != null && company.getTanNumber() != null) ? company.getTanNumber() : "-";
        String branchAddress = (branch != null)
                ? (branch.getAddressLine() + ", " + branch.getCity() + ", " + branch.getStateCode() + " - " + branch.getPincode())
                : "-";

        String monthName = Month.of(run.getPayrollMonth()).getDisplayName(TextStyle.FULL, Locale.ROOT);
        String periodTitle = String.format(Locale.ROOT, "PAYSLIP FOR %s %d",
                monthName.toUpperCase(Locale.ROOT), run.getPayrollYear());

        String empCode = emp.getEmployeeCode() != null ? emp.getEmployeeCode() : "-";
        String empName = emp.getFirstName() + (emp.getLastName() != null && !emp.getLastName().isBlank() ? " " + emp.getLastName() : "");
        String designation = emp.getDesignation() != null ? emp.getDesignation() : "-";
        String department = emp.getDepartment() != null ? emp.getDepartment() : "-";
        String doj = emp.getDateOfJoining() != null ? emp.getDateOfJoining().format(DATE_FORMAT) : "-";

        String bankAcc = (vault != null && vault.getBankAccountNumber() != null)
                ? KycVaultService.maskBankAccount(vault.getBankAccountNumber()) : "******";
        String bankIfsc = (vault != null && vault.getBankIfsc() != null) ? vault.getBankIfsc() : "-";
        String pan = (vault != null && vault.getPanNumber() != null) ? vault.getPanNumber() : "-";
        String uan = (vault != null && vault.getUan() != null && !vault.getUan().isBlank()) ? vault.getUan() : "-";

        int calendarDays = (attendance != null && attendance.getTotalCalendarDays() != null)
                ? attendance.getTotalCalendarDays()
                : YearMonth.of(run.getPayrollYear(), run.getPayrollMonth()).lengthOfMonth();
        BigDecimal payableDays = (attendance != null && attendance.getPayableDays() != null)
                ? attendance.getPayableDays() : BigDecimal.valueOf(calendarDays);
        BigDecimal lopDays = (attendance != null && attendance.getLopDays() != null)
                ? attendance.getLopDays() : BigDecimal.ZERO;

        List<PayslipLineItem> earnings = items.stream()
                .filter(i -> "EARNING".equalsIgnoreCase(i.getItemCategory()))
                .toList();
        List<PayslipLineItem> deductions = items.stream()
                .filter(i -> !"EARNING".equalsIgnoreCase(i.getItemCategory()))
                .toList();

        BigDecimal totalGross = earnings.stream()
                .map(PayslipLineItem::getItemAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalDeductions = deductions.stream()
                .map(PayslipLineItem::getItemAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal netPay = totalGross.subtract(totalDeductions).setScale(2, RoundingMode.HALF_UP);
        String netPayWords = IndianCurrencyWordConverter.convertToWords(netPay);

        String signatoryName = (company != null && company.getSignatoryName() != null)
                ? company.getSignatoryName() : "Authorized Signatory";
        String signatoryDesig = (company != null && company.getSignatoryDesignation() != null)
                ? company.getSignatoryDesignation() : "Director";

        StringBuilder stream = new StringBuilder();

        // 1. Header Box
        stream.append("BT /F2 14 Tf 40 800 Td (").append(escapePdf(companyLegalName)).append(") Tj ET\n");
        if (!companyTradeName.isBlank()) {
            stream.append("BT /F1 9 Tf 40 788 Td (").append(escapePdf(companyTradeName)).append(") Tj ET\n");
        }
        stream.append("BT /F1 8 Tf 40 774 Td (GSTIN: ").append(escapePdf(gstin))
                .append("  |  TAN: ").append(escapePdf(tan)).append(") Tj ET\n");
        stream.append("BT /F1 8 Tf 40 762 Td (Address: ").append(escapePdf(branchAddress)).append(") Tj ET\n");

        // Divider
        stream.append("q 0.7 0.7 0.7 RG 1 w 40 752 m 555 752 l S Q\n");

        // 2. Pay Period Banner
        stream.append("q 0.9 0.9 0.95 rg 40 728 515 18 re f Q\n");
        stream.append("BT /F2 10 Tf 50 733 Td (").append(escapePdf(periodTitle)).append(") Tj ET\n");

        // 3. Employee & Bank Info Box
        stream.append("q 0.8 0.8 0.8 RG 0.5 w 40 640 515 82 re S Q\n");

        // Left Column
        stream.append("BT /F2 8 Tf 48 710 Td (Employee Code:) Tj ET\n");
        stream.append("BT /F1 8 Tf 120 710 Td (").append(escapePdf(empCode)).append(") Tj ET\n");

        stream.append("BT /F2 8 Tf 48 696 Td (Employee Name:) Tj ET\n");
        stream.append("BT /F1 8 Tf 120 696 Td (").append(escapePdf(empName)).append(") Tj ET\n");

        stream.append("BT /F2 8 Tf 48 682 Td (Designation:) Tj ET\n");
        stream.append("BT /F1 8 Tf 120 682 Td (").append(escapePdf(designation)).append(") Tj ET\n");

        stream.append("BT /F2 8 Tf 48 668 Td (Department:) Tj ET\n");
        stream.append("BT /F1 8 Tf 120 668 Td (").append(escapePdf(department)).append(") Tj ET\n");

        stream.append("BT /F2 8 Tf 48 654 Td (Date of Join:) Tj ET\n");
        stream.append("BT /F1 8 Tf 120 654 Td (").append(escapePdf(doj)).append(") Tj ET\n");

        // Right Column
        stream.append("BT /F2 8 Tf 310 710 Td (Bank Account:) Tj ET\n");
        stream.append("BT /F1 8 Tf 385 710 Td (").append(escapePdf(bankAcc)).append(") Tj ET\n");

        stream.append("BT /F2 8 Tf 310 696 Td (Bank IFSC:) Tj ET\n");
        stream.append("BT /F1 8 Tf 385 696 Td (").append(escapePdf(bankIfsc)).append(") Tj ET\n");

        stream.append("BT /F2 8 Tf 310 682 Td (PAN Number:) Tj ET\n");
        stream.append("BT /F1 8 Tf 385 682 Td (").append(escapePdf(pan)).append(") Tj ET\n");

        stream.append("BT /F2 8 Tf 310 668 Td (UAN:) Tj ET\n");
        stream.append("BT /F1 8 Tf 385 668 Td (").append(escapePdf(uan)).append(") Tj ET\n");

        // 4. Attendance Summary Box
        stream.append("q 0.93 0.93 0.93 rg 40 618 515 16 re f Q\n");
        String attendanceStr = String.format(Locale.ROOT,
                "Calendar Days: %d        Payable Days: %s        LOP Days: %s",
                calendarDays, payableDays.toPlainString(), lopDays.toPlainString());
        stream.append("BT /F2 8 Tf 48 623 Td (").append(escapePdf(attendanceStr)).append(") Tj ET\n");

        // 5. Earnings & Deductions Table
        // Table Header
        stream.append("q 0.85 0.85 0.9 rg 40 594 515 18 re f Q\n");
        stream.append("BT /F2 9 Tf 48 599 Td (EARNINGS) Tj ET\n");
        stream.append("BT /F2 9 Tf 220 599 Td (AMOUNT (INR)) Tj ET\n");
        stream.append("BT /F2 9 Tf 310 599 Td (DEDUCTIONS) Tj ET\n");
        stream.append("BT /F2 9 Tf 480 599 Td (AMOUNT (INR)) Tj ET\n");

        // Table Rows
        int maxRows = Math.max(earnings.size(), deductions.size());
        int tableY = 578;
        int rowHeight = 16;

        for (int i = 0; i < maxRows; i++) {
            if (i < earnings.size()) {
                PayslipLineItem earn = earnings.get(i);
                stream.append("BT /F1 8 Tf 48 ").append(tableY).append(" Td (")
                        .append(escapePdf(earn.getItemName())).append(") Tj ET\n");
                stream.append("BT /F1 8 Tf 220 ").append(tableY).append(" Td (")
                        .append(earn.getItemAmount().setScale(2, RoundingMode.HALF_UP).toPlainString()).append(") Tj ET\n");
            }

            if (i < deductions.size()) {
                PayslipLineItem ded = deductions.get(i);
                stream.append("BT /F1 8 Tf 310 ").append(tableY).append(" Td (")
                        .append(escapePdf(ded.getItemName())).append(") Tj ET\n");
                stream.append("BT /F1 8 Tf 480 ").append(tableY).append(" Td (")
                        .append(ded.getItemAmount().setScale(2, RoundingMode.HALF_UP).toPlainString()).append(") Tj ET\n");
            }
            tableY -= rowHeight;
        }

        // Table Outer border and divider
        int tableHeight = 18 + (maxRows * rowHeight) + 18;
        stream.append("q 0.8 0.8 0.8 RG 0.5 w 40 ").append(tableY - 18).append(" 515 ").append(tableHeight).append(" re S Q\n");
        stream.append("q 0.8 0.8 0.8 RG 0.5 w 295 ").append(tableY - 18).append(" m 295 612 l S Q\n");

        // Totals Row
        stream.append("q 0.9 0.9 0.9 rg 40 ").append(tableY - 18).append(" 515 18 re f Q\n");
        stream.append("BT /F2 8 Tf 48 ").append(tableY - 13).append(" Td (Total Earnings: INR ")
                .append(totalGross.toPlainString()).append(") Tj ET\n");
        stream.append("BT /F2 8 Tf 310 ").append(tableY - 13).append(" Td (Total Deductions: INR ")
                .append(totalDeductions.toPlainString()).append(") Tj ET\n");

        // 6. Net Pay Box
        int netPayBoxY = tableY - 70;
        stream.append("q 0.92 0.96 0.92 rg 40 ").append(netPayBoxY).append(" 515 44 re f Q\n");
        stream.append("q 0.3 0.6 0.3 RG 1 w 40 ").append(netPayBoxY).append(" 515 44 re S Q\n");

        stream.append("BT /F2 11 Tf 50 ").append(netPayBoxY + 26).append(" Td (NET SALARY PAYABLE: INR ")
                .append(netPay.toPlainString()).append(") Tj ET\n");
        stream.append("BT /F1 8 Tf 50 ").append(netPayBoxY + 10).append(" Td (In Words: ")
                .append(escapePdf(netPayWords)).append(") Tj ET\n");

        // 7. Footer & Signatory
        int footerY = netPayBoxY - 45;
        stream.append("BT /F2 8 Tf 40 ").append(footerY).append(" Td (Authorized Signatory: ")
                .append(escapePdf(signatoryName)).append(" \\(")
                .append(escapePdf(signatoryDesig)).append("\\)) Tj ET\n");
        stream.append("BT /F1 7 Tf 40 ").append(footerY - 14).append(" Td (This is a system-generated payslip and does not require a physical signature.) Tj ET\n");

        return renderPdfDocument(stream.toString());
    }

    private byte[] renderPdfDocument(String contentStream) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            byte[] header = new byte[]{'%', 'P', 'D', 'F', '-', '1', '.', '4', '\n', '%', (byte) 0xE2, (byte) 0xE3, (byte) 0xCF, (byte) 0xD3, '\n'};
            out.write(header);

            long[] offsets = new long[7];

            // 1 0 obj: Catalog
            offsets[1] = out.size();
            out.write("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n".getBytes(StandardCharsets.ISO_8859_1));

            // 2 0 obj: Pages
            offsets[2] = out.size();
            out.write("2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n".getBytes(StandardCharsets.ISO_8859_1));

            // 3 0 obj: Page
            offsets[3] = out.size();
            String pageObj = "3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Contents 4 0 R /Resources << /Font << /F1 5 0 R /F2 6 0 R >> >> >>\nendobj\n";
            out.write(pageObj.getBytes(StandardCharsets.ISO_8859_1));

            // 4 0 obj: Content Stream
            offsets[4] = out.size();
            byte[] streamBytes = contentStream.getBytes(StandardCharsets.ISO_8859_1);
            String streamObjHead = "4 0 obj\n<< /Length " + streamBytes.length + " >>\nstream\n";
            out.write(streamObjHead.getBytes(StandardCharsets.ISO_8859_1));
            out.write(streamBytes);
            out.write("\nendstream\nendobj\n".getBytes(StandardCharsets.ISO_8859_1));

            // 5 0 obj: Font F1
            offsets[5] = out.size();
            out.write("5 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n".getBytes(StandardCharsets.ISO_8859_1));

            // 6 0 obj: Font F2
            offsets[6] = out.size();
            out.write("6 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold >>\nendobj\n".getBytes(StandardCharsets.ISO_8859_1));

            // xref
            long startXref = out.size();
            out.write("xref\n0 7\n0000000000 65535 f \n".getBytes(StandardCharsets.ISO_8859_1));
            for (int i = 1; i <= 6; i++) {
                String line = String.format(Locale.ROOT, "%010d 00000 n \n", offsets[i]);
                out.write(line.getBytes(StandardCharsets.ISO_8859_1));
            }

            // trailer
            String trailer = "trailer\n<< /Size 7 /Root 1 0 R >>\nstartxref\n" + startXref + "\n%%EOF\n";
            out.write(trailer.getBytes(StandardCharsets.ISO_8859_1));

        } catch (IOException ex) {
            throw new IllegalStateException("Failed to render PDF document", ex);
        }

        return out.toByteArray();
    }

    private String escapePdf(String text) {
        if (text == null) {
            return "";
        }
        // Retain only ASCII characters, sanitize special PDF chars
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (c == '\\') {
                sb.append("\\\\");
            } else if (c == '(') {
                sb.append("\\(");
            } else if (c == ')') {
                sb.append("\\)");
            } else if (c >= 32 && c <= 126) {
                sb.append(c);
            } else {
                sb.append(' ');
            }
        }
        return sb.toString();
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
}

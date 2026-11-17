package com.ourhr.exporter.ecr;

import com.ourhr.attendance.MonthlyAttendanceSummary;
import com.ourhr.attendance.MonthlyAttendanceSummaryRepository;
import com.ourhr.common.ResourceNotFoundException;
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
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Generates official EPFO ECR 2.0 text return files for processed payroll runs.
 * Produces 25 fields per employee record delimited by '#~#'.
 */
@Service
@RequiredArgsConstructor
public class EcrExporterService {

    private static final Logger log = LoggerFactory.getLogger(EcrExporterService.class);

    public static final String DELIMITER = "#~#";
    private static final BigDecimal DEFAULT_EPF_CEILING = new BigDecimal("15000.00");
    private static final BigDecimal EPS_MAX_CAP = new BigDecimal("1250.00");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private final PayrollRunRepository payrollRunRepository;
    private final PayslipLineItemRepository payslipLineItemRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeKycVaultRepository employeeKycVaultRepository;
    private final MonthlyAttendanceSummaryRepository monthlyAttendanceSummaryRepository;
    private final SalaryStructureService salaryStructureService;
    private final CompanyRepository companyRepository;
    private final StatutoryQueryService statutoryQueryService;
    private final FileStorageService fileStorageService;

    @Transactional
    public EcrExportResponse generateEcrFile(UUID payrollRunId) {
        UUID companyId = resolveCurrentCompanyId();
        PayrollRun run = getPayrollRunOrThrow(companyId, payrollRunId);

        String establishmentCode = resolveEstablishmentCode(companyId);
        List<EcrRowData> rows = buildEcrRows(companyId, run);

        BigDecimal totalGross = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalEpf = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalEe = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalEps = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalEr = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        StringBuilder sb = new StringBuilder();
        for (EcrRowData row : rows) {
            sb.append(row.toDelimitedString()).append("\n");
            totalGross = totalGross.add(row.grossWages);
            totalEpf = totalEpf.add(row.epfWages);
            totalEe = totalEe.add(row.eeShare);
            totalEps = totalEps.add(row.epsShare);
            totalEr = totalEr.add(row.erShare);
        }

        byte[] contentBytes = sb.toString().getBytes(StandardCharsets.UTF_8);
        String fileName = String.format(Locale.ROOT, "ECR_%s_%02d_%04d.txt",
                establishmentCode, run.getPayrollMonth(), run.getPayrollYear());

        String fileKey = fileStorageService.uploadFile("ecr", fileName, contentBytes, "text/plain");
        String downloadUrl = fileStorageService.generatePresignedUrl(fileKey, 86400L);

        run.setEcrTxtFileUrl(downloadUrl);
        payrollRunRepository.save(run);

        log.info("Successfully generated EPFO ECR file for run ID: {}, members: {}, total gross: {}",
                payrollRunId, rows.size(), totalGross);

        return EcrExportResponse.builder()
                .payrollRunId(payrollRunId)
                .establishmentCode(establishmentCode)
                .payrollMonth(run.getPayrollMonth())
                .payrollYear(run.getPayrollYear())
                .fileName(fileName)
                .fileDownloadUrl(downloadUrl)
                .memberCount(rows.size())
                .totalGrossWages(totalGross)
                .totalEpfWages(totalEpf)
                .totalEeShare(totalEe)
                .totalEpsShare(totalEps)
                .totalErShare(totalEr)
                .generatedAt(Instant.now())
                .build();
    }

    @Transactional(readOnly = true)
    public byte[] generateEcrBytes(UUID payrollRunId) {
        return generateEcrText(payrollRunId).getBytes(StandardCharsets.UTF_8);
    }

    @Transactional(readOnly = true)
    public String generateEcrText(UUID payrollRunId) {
        UUID companyId = resolveCurrentCompanyId();
        PayrollRun run = getPayrollRunOrThrow(companyId, payrollRunId);
        List<EcrRowData> rows = buildEcrRows(companyId, run);

        StringBuilder sb = new StringBuilder();
        for (EcrRowData row : rows) {
            sb.append(row.toDelimitedString()).append("\n");
        }
        return sb.toString();
    }

    private List<EcrRowData> buildEcrRows(UUID companyId, PayrollRun run) {
        LocalDate queryDate = YearMonth.of(run.getPayrollYear(), run.getPayrollMonth()).atEndOfMonth();
        BigDecimal epfCeiling = resolveEpfCeiling(queryDate);

        List<PayslipLineItem> items = payslipLineItemRepository
                .findByCompanyIdAndPayrollRunId(companyId, run.getId());

        Map<UUID, List<PayslipLineItem>> itemsByEmployee = items.stream()
                .collect(Collectors.groupingBy(PayslipLineItem::getEmployeeId));

        List<EcrRowData> result = new ArrayList<>();

        for (Map.Entry<UUID, List<PayslipLineItem>> entry : itemsByEmployee.entrySet()) {
            UUID employeeId = entry.getKey();
            List<PayslipLineItem> empItems = entry.getValue();

            Optional<Employee> empOpt = employeeRepository.findByIdAndCompanyId(employeeId, companyId);
            if (empOpt.isEmpty()) {
                continue;
            }
            Employee emp = empOpt.get();

            if ("CONSULTANT_194J".equalsIgnoreCase(emp.getWorkerType())
                    || "CONTRACTOR_194C".equalsIgnoreCase(emp.getWorkerType())) {
                continue;
            }

            try {
                Optional<EmployeeSalaryStructure> structureOpt =
                        salaryStructureService.findActiveStructure(employeeId, queryDate);
                if (structureOpt.isPresent() && Boolean.TRUE.equals(structureOpt.get().getPfOptOut())) {
                    continue;
                }
            } catch (Exception ignored) {
            }

            Optional<EmployeeKycVault> vaultOpt =
                    employeeKycVaultRepository.findByCompanyIdAndEmployeeId(companyId, employeeId);

            Optional<MonthlyAttendanceSummary> attendanceOpt =
                    monthlyAttendanceSummaryRepository.findByCompanyIdAndEmployeeIdAndPayrollMonthAndPayrollYear(
                            companyId, employeeId, run.getPayrollMonth(), run.getPayrollYear());

            BigDecimal gross = empItems.stream()
                    .filter(i -> "EARNING".equalsIgnoreCase(i.getItemCategory()))
                    .map(PayslipLineItem::getItemAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);

            BigDecimal basic = empItems.stream()
                    .filter(i -> "EARNING".equalsIgnoreCase(i.getItemCategory())
                            && i.getItemName() != null
                            && i.getItemName().toLowerCase(Locale.ROOT).contains("basic"))
                    .map(PayslipLineItem::getItemAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);

            BigDecimal da = empItems.stream()
                    .filter(i -> "EARNING".equalsIgnoreCase(i.getItemCategory())
                            && i.getItemName() != null
                            && (i.getItemName().toLowerCase(Locale.ROOT).contains("dearness")
                            || i.getItemName().equalsIgnoreCase("da")))
                    .map(PayslipLineItem::getItemAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);

            BigDecimal basicPlusDa = basic.add(da);
            if (basicPlusDa.compareTo(BigDecimal.ZERO) <= 0 && gross.compareTo(BigDecimal.ZERO) > 0) {
                basicPlusDa = gross;
            }

            BigDecimal epfWage = basicPlusDa.min(epfCeiling).setScale(2, RoundingMode.HALF_UP);
            BigDecimal epsWage = epfWage;
            BigDecimal edliWage = epfWage;

            BigDecimal eeShare = epfWage.multiply(new BigDecimal("0.12")).setScale(2, RoundingMode.HALF_UP);

            BigDecimal epsShare;
            if (epfWage.compareTo(epfCeiling) >= 0) {
                epsShare = EPS_MAX_CAP;
            } else {
                epsShare = epfWage.multiply(new BigDecimal("0.0833")).setScale(2, RoundingMode.HALF_UP).min(EPS_MAX_CAP);
            }

            BigDecimal erShare = eeShare.subtract(epsShare).setScale(2, RoundingMode.HALF_UP);

            BigDecimal lopDays = attendanceOpt.map(MonthlyAttendanceSummary::getLopDays).orElse(BigDecimal.ZERO);
            int ncpDays = lopDays != null ? lopDays.intValue() : 0;

            String uan = vaultOpt.map(EmployeeKycVault::getUan).filter(u -> u != null && !u.isBlank()).orElse("");
            String firstName = emp.getFirstName() != null ? emp.getFirstName().trim() : "";
            String lastName = emp.getLastName() != null ? emp.getLastName().trim() : "";
            String memberName = (firstName + (lastName.isBlank() ? "" : " " + lastName)).trim();

            String dob = emp.getDateOfBirth() != null ? emp.getDateOfBirth().format(DATE_FORMAT) : "";
            String gender = (emp.getGender() != null && !emp.getGender().isBlank())
                    ? emp.getGender().trim().substring(0, 1).toUpperCase(Locale.ROOT)
                    : "M";

            String doj = emp.getDateOfJoining() != null ? emp.getDateOfJoining().format(DATE_FORMAT) : "";

            EcrRowData rowData = EcrRowData.builder()
                    .uan(uan)
                    .memberName(memberName)
                    .grossWages(gross)
                    .epfWages(epfWage)
                    .epsWages(epsWage)
                    .edliWages(edliWage)
                    .eeShare(eeShare)
                    .epsShare(epsShare)
                    .erShare(erShare)
                    .ncpDays(ncpDays)
                    .totalAdvances("0")
                    .arrearEpfWages("0")
                    .arrearEeShare("0")
                    .arrearErShare("0")
                    .arrearEpsShare("0")
                    .fatherOrHusbandName("-")
                    .relationship("F")
                    .dateOfBirth(dob)
                    .gender(gender)
                    .dojEpf(doj)
                    .dojEps(doj)
                    .dateOfExitEpf("")
                    .dateOfExitEps("")
                    .reasonForExit("")
                    .pmrpyBeneficiary("N")
                    .build();

            result.add(rowData);
        }

        return result;
    }

    private BigDecimal resolveEpfCeiling(LocalDate onDate) {
        try {
            BigDecimal ceiling = statutoryQueryService.getEpfWageCeiling(onDate);
            if (ceiling != null && ceiling.compareTo(BigDecimal.ZERO) > 0) {
                return ceiling;
            }
        } catch (Exception ignored) {
        }
        return DEFAULT_EPF_CEILING;
    }

    private String resolveEstablishmentCode(UUID companyId) {
        return companyRepository.findById(companyId)
                .map(Company::getPfEstablishmentCode)
                .filter(code -> code != null && !code.isBlank())
                .orElse("EST" + companyId.toString().replace("-", "").substring(0, 7).toUpperCase(Locale.ROOT));
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

    @lombok.Builder
    public static class EcrRowData {
        public String uan;
        public String memberName;
        public BigDecimal grossWages;
        public BigDecimal epfWages;
        public BigDecimal epsWages;
        public BigDecimal edliWages;
        public BigDecimal eeShare;
        public BigDecimal epsShare;
        public BigDecimal erShare;
        public int ncpDays;
        public String totalAdvances;
        public String arrearEpfWages;
        public String arrearEeShare;
        public String arrearErShare;
        public String arrearEpsShare;
        public String fatherOrHusbandName;
        public String relationship;
        public String dateOfBirth;
        public String gender;
        public String dojEpf;
        public String dojEps;
        public String dateOfExitEpf;
        public String dateOfExitEps;
        public String reasonForExit;
        public String pmrpyBeneficiary;

        public String toDelimitedString() {
            return String.join(DELIMITER,
                    uan != null ? uan : "",
                    memberName != null ? memberName : "",
                    grossWages.setScale(2, RoundingMode.HALF_UP).toPlainString(),
                    epfWages.setScale(2, RoundingMode.HALF_UP).toPlainString(),
                    epsWages.setScale(2, RoundingMode.HALF_UP).toPlainString(),
                    edliWages.setScale(2, RoundingMode.HALF_UP).toPlainString(),
                    eeShare.setScale(2, RoundingMode.HALF_UP).toPlainString(),
                    epsShare.setScale(2, RoundingMode.HALF_UP).toPlainString(),
                    erShare.setScale(2, RoundingMode.HALF_UP).toPlainString(),
                    String.valueOf(ncpDays),
                    totalAdvances != null ? totalAdvances : "0",
                    arrearEpfWages != null ? arrearEpfWages : "0",
                    arrearEeShare != null ? arrearEeShare : "0",
                    arrearErShare != null ? arrearErShare : "0",
                    arrearEpsShare != null ? arrearEpsShare : "0",
                    fatherOrHusbandName != null ? fatherOrHusbandName : "-",
                    relationship != null ? relationship : "F",
                    dateOfBirth != null ? dateOfBirth : "",
                    gender != null ? gender : "M",
                    dojEpf != null ? dojEpf : "",
                    dojEps != null ? dojEps : "",
                    dateOfExitEpf != null ? dateOfExitEpf : "",
                    dateOfExitEps != null ? dateOfExitEps : "",
                    reasonForExit != null ? reasonForExit : "",
                    pmrpyBeneficiary != null ? pmrpyBeneficiary : "N"
            );
        }
    }
}

package com.ourhr.payroll;

import com.ourhr.employee.Employee;
import com.ourhr.employee.EmployeeRepository;
import com.ourhr.payroll.dto.PayrollAuditDiffResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PayrollAuditService {

    private final PayrollRunRepository payrollRunRepository;
    private final PayslipLineItemRepository payslipLineItemRepository;
    private final EmployeeRepository employeeRepository;

    @Transactional(readOnly = true)
    public List<PayrollAuditDiffResponse> auditRun(PayrollRun currentRun) {
        UUID companyId = currentRun.getCompanyId();

        int currentMonth = currentRun.getPayrollMonth();
        int currentYear = currentRun.getPayrollYear();
        int prevMonth = (currentMonth == 1) ? 12 : currentMonth - 1;
        int prevYear = (currentMonth == 1) ? currentYear - 1 : currentYear;

        Optional<PayrollRun> previousRunOpt = payrollRunRepository
                .findByCompanyIdAndPayrollMonthAndPayrollYear(companyId, prevMonth, prevYear);

        List<PayslipLineItem> currentItems = payslipLineItemRepository
                .findByCompanyIdAndPayrollRunId(companyId, currentRun.getId());

        Map<UUID, BigDecimal> currentNetMap = calculateNetPayMap(currentItems);
        Map<UUID, BigDecimal> prevNetMap = previousRunOpt
                .map(prevRun -> calculateNetPayMap(
                        payslipLineItemRepository.findByCompanyIdAndPayrollRunId(companyId, prevRun.getId())))
                .orElse(Map.of());

        Set<UUID> allEmployeeIds = new HashSet<>(currentNetMap.keySet());
        allEmployeeIds.addAll(prevNetMap.keySet());

        List<PayrollAuditDiffResponse> diffList = new ArrayList<>();

        for (UUID empId : allEmployeeIds) {
            BigDecimal currentNet = currentNetMap.getOrDefault(empId, BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
            BigDecimal prevNet = prevNetMap.getOrDefault(empId, BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
            BigDecimal diff = currentNet.subtract(prevNet).setScale(2, RoundingMode.HALF_UP);

            String flagReason;
            if (previousRunOpt.isEmpty()) {
                flagReason = "INITIAL_RUN";
            } else if (prevNet.compareTo(BigDecimal.ZERO) == 0 && currentNet.compareTo(BigDecimal.ZERO) > 0) {
                flagReason = "NEW_JOINER";
            } else if (currentNet.compareTo(BigDecimal.ZERO) == 0 && prevNet.compareTo(BigDecimal.ZERO) > 0) {
                flagReason = "EXITED_EMPLOYEE";
            } else if (diff.compareTo(BigDecimal.ZERO) > 0) {
                flagReason = "SALARY_INCREASE";
            } else if (diff.compareTo(BigDecimal.ZERO) < 0) {
                flagReason = "SALARY_DECREASE";
            } else {
                flagReason = "NO_CHANGE";
            }

            Optional<Employee> empOpt = employeeRepository.findByIdAndCompanyId(empId, companyId);
            String code = empOpt.map(Employee::getEmployeeCode).orElse("UNKNOWN");
            String name = empOpt.map(e -> e.getFirstName() + " " + e.getLastName()).orElse("Unknown Employee");

            diffList.add(PayrollAuditDiffResponse.builder()
                    .employeeId(empId)
                    .employeeCode(code)
                    .employeeName(name)
                    .previousNetPay(prevNet)
                    .currentNetPay(currentNet)
                    .netPayDifference(diff)
                    .auditFlagReason(flagReason)
                    .build());
        }

        diffList.sort(Comparator.comparing(PayrollAuditDiffResponse::getEmployeeCode));
        return diffList;
    }

    private Map<UUID, BigDecimal> calculateNetPayMap(List<PayslipLineItem> items) {
        Map<UUID, BigDecimal> netMap = new HashMap<>();
        for (PayslipLineItem item : items) {
            UUID empId = item.getEmployeeId();
            BigDecimal current = netMap.getOrDefault(empId, BigDecimal.ZERO);
            if ("EARNING".equalsIgnoreCase(item.getItemCategory())) {
                netMap.put(empId, current.add(item.getItemAmount()));
            } else if ("STATUTORY_DEDUCTION".equalsIgnoreCase(item.getItemCategory())
                    || "OTHER_DEDUCTION".equalsIgnoreCase(item.getItemCategory())) {
                netMap.put(empId, current.subtract(item.getItemAmount()));
            }
        }
        return netMap;
    }
}

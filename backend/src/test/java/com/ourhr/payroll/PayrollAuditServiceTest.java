package com.ourhr.payroll;

import com.ourhr.employee.Employee;
import com.ourhr.employee.EmployeeRepository;
import com.ourhr.payroll.dto.PayrollAuditDiffResponse;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayrollAuditServiceTest {

    @Mock
    private PayrollRunRepository payrollRunRepository;

    @Mock
    private PayslipLineItemRepository payslipLineItemRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private PayrollAuditService payrollAuditService;

    @Test
    @DisplayName("Should flag INITIAL_RUN when no previous payroll run exists")
    void shouldFlagInitialRun() {
        UUID companyId = UUID.randomUUID();
        UUID runId = UUID.randomUUID();
        UUID empId = UUID.randomUUID();

        PayrollRun currentRun = PayrollRun.builder()
                .payrollMonth(4)
                .payrollYear(2026)
                .build();
        currentRun.setId(runId);
        currentRun.setCompanyId(companyId);

        when(payrollRunRepository.findByCompanyIdAndPayrollMonthAndPayrollYear(companyId, 3, 2026))
                .thenReturn(Optional.empty());

        PayslipLineItem earning = PayslipLineItem.builder()
                .payrollRunId(runId)
                .employeeId(empId)
                .itemCategory("EARNING")
                .itemName("Basic Pay")
                .itemAmount(new BigDecimal("50000.00"))
                .build();

        when(payslipLineItemRepository.findByCompanyIdAndPayrollRunId(companyId, runId))
                .thenReturn(List.of(earning));

        Employee emp = Employee.builder()
                .employeeCode("EMP001")
                .firstName("Mahesh")
                .lastName("Ch")
                .build();
        emp.setId(empId);
        emp.setCompanyId(companyId);

        when(employeeRepository.findByIdAndCompanyId(empId, companyId)).thenReturn(Optional.of(emp));

        List<PayrollAuditDiffResponse> diffs = payrollAuditService.auditRun(currentRun);

        assertThat(diffs).hasSize(1);
        PayrollAuditDiffResponse diff = diffs.get(0);
        assertThat(diff.getAuditFlagReason()).isEqualTo("INITIAL_RUN");
        assertThat(diff.getCurrentNetPay()).isEqualByComparingTo(new BigDecimal("50000.00"));
        assertThat(diff.getPreviousNetPay()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Should detect salary changes, new joiners, and exited employees against previous month")
    void shouldDetectVariations() {
        UUID companyId = UUID.randomUUID();
        UUID currentRunId = UUID.randomUUID();
        UUID prevRunId = UUID.randomUUID();

        UUID empExisting = UUID.randomUUID();
        UUID empNew = UUID.randomUUID();
        UUID empExited = UUID.randomUUID();

        PayrollRun currentRun = PayrollRun.builder()
                .payrollMonth(5)
                .payrollYear(2026)
                .build();
        currentRun.setId(currentRunId);
        currentRun.setCompanyId(companyId);

        PayrollRun prevRun = PayrollRun.builder()
                .payrollMonth(4)
                .payrollYear(2026)
                .build();
        prevRun.setId(prevRunId);
        prevRun.setCompanyId(companyId);

        when(payrollRunRepository.findByCompanyIdAndPayrollMonthAndPayrollYear(companyId, 4, 2026))
                .thenReturn(Optional.of(prevRun));

        // Current run items: empExisting (60,000 net) and empNew (40,000 net)
        PayslipLineItem item1 = PayslipLineItem.builder()
                .payrollRunId(currentRunId).employeeId(empExisting).itemCategory("EARNING").itemAmount(new BigDecimal("60000.00")).build();
        PayslipLineItem item2 = PayslipLineItem.builder()
                .payrollRunId(currentRunId).employeeId(empNew).itemCategory("EARNING").itemAmount(new BigDecimal("40000.00")).build();
        when(payslipLineItemRepository.findByCompanyIdAndPayrollRunId(companyId, currentRunId))
                .thenReturn(List.of(item1, item2));

        // Previous run items: empExisting (50,000 net) and empExited (30,000 net)
        PayslipLineItem prevItem1 = PayslipLineItem.builder()
                .payrollRunId(prevRunId).employeeId(empExisting).itemCategory("EARNING").itemAmount(new BigDecimal("50000.00")).build();
        PayslipLineItem prevItem2 = PayslipLineItem.builder()
                .payrollRunId(prevRunId).employeeId(empExited).itemCategory("EARNING").itemAmount(new BigDecimal("30000.00")).build();
        when(payslipLineItemRepository.findByCompanyIdAndPayrollRunId(companyId, prevRunId))
                .thenReturn(List.of(prevItem1, prevItem2));

        Employee e1 = Employee.builder().employeeCode("EMP001").firstName("Existing").lastName("User").build();
        Employee e2 = Employee.builder().employeeCode("EMP002").firstName("New").lastName("Joiner").build();
        Employee e3 = Employee.builder().employeeCode("EMP003").firstName("Exited").lastName("User").build();

        when(employeeRepository.findByIdAndCompanyId(empExisting, companyId)).thenReturn(Optional.of(e1));
        when(employeeRepository.findByIdAndCompanyId(empNew, companyId)).thenReturn(Optional.of(e2));
        when(employeeRepository.findByIdAndCompanyId(empExited, companyId)).thenReturn(Optional.of(e3));

        List<PayrollAuditDiffResponse> diffs = payrollAuditService.auditRun(currentRun);

        assertThat(diffs).hasSize(3);

        PayrollAuditDiffResponse existingDiff = diffs.stream().filter(d -> d.getEmployeeId().equals(empExisting)).findFirst().orElseThrow();
        assertThat(existingDiff.getAuditFlagReason()).isEqualTo("SALARY_INCREASE");
        assertThat(existingDiff.getNetPayDifference()).isEqualByComparingTo(new BigDecimal("10000.00"));

        PayrollAuditDiffResponse newDiff = diffs.stream().filter(d -> d.getEmployeeId().equals(empNew)).findFirst().orElseThrow();
        assertThat(newDiff.getAuditFlagReason()).isEqualTo("NEW_JOINER");

        PayrollAuditDiffResponse exitedDiff = diffs.stream().filter(d -> d.getEmployeeId().equals(empExited)).findFirst().orElseThrow();
        assertThat(exitedDiff.getAuditFlagReason()).isEqualTo("EXITED_EMPLOYEE");
    }
}

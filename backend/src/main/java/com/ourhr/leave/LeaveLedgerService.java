package com.ourhr.leave;

import com.ourhr.attendance.HolidayCalendar;
import com.ourhr.attendance.HolidayCalendarRepository;
import com.ourhr.auth.User;
import com.ourhr.auth.UserRepository;
import com.ourhr.company.BranchRepository;
import com.ourhr.common.ResourceNotFoundException;
import com.ourhr.common.TenantContext;
import com.ourhr.employee.Employee;
import com.ourhr.employee.EmployeeRepository;
import com.ourhr.leave.dto.ApplyLeaveRequest;
import com.ourhr.leave.dto.CreateLeaveTypeRequest;
import com.ourhr.leave.dto.LeaveApplicationResponse;
import com.ourhr.leave.dto.LeaveBalanceResponse;
import com.ourhr.leave.dto.LeaveTypeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LeaveLedgerService {

    private final LeaveLedgerRepository leaveLedgerRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveApplicationRepository leaveApplicationRepository;
    private final HolidayCalendarRepository holidayCalendarRepository;
    private final BranchRepository branchRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;

    @Transactional
    public LeaveTypeResponse createLeaveType(CreateLeaveTypeRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant context is required");
        }

        String leaveCode = request.getLeaveCode().trim().toUpperCase();
        leaveTypeRepository.findByCompanyIdAndLeaveCode(tenantId, leaveCode).ifPresent(existing -> {
            throw new IllegalArgumentException("Leave type with code '" + leaveCode + "' already exists");
        });

        LeaveType leaveType = LeaveType.builder()
                .leaveName(request.getLeaveName().trim())
                .leaveCode(leaveCode)
                .annualQuota(request.getAnnualQuota())
                .accrualFrequency(request.getAccrualFrequency().trim().toUpperCase())
                .isCarryForwardAllowed(request.isCarryForwardAllowed())
                .maxCarryForwardLimit(request.getMaxCarryForwardLimit() != null ? request.getMaxCarryForwardLimit() : new BigDecimal("30.0"))
                .isSandwichRuleEnabled(request.isSandwichRuleEnabled())
                .build();
        leaveType.setCompanyId(tenantId);

        LeaveType saved = leaveTypeRepository.save(leaveType);
        return toLeaveTypeResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<LeaveTypeResponse> getAllLeaveTypes() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant context is required");
        }

        return leaveTypeRepository.findByCompanyId(tenantId)
                .stream()
                .map(this::toLeaveTypeResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BigDecimal getLeaveBalance(UUID employeeId, UUID leaveTypeId) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant context is required");
        }

        BigDecimal balance = leaveLedgerRepository.getBalance(tenantId, employeeId, leaveTypeId);
        return balance != null ? balance.setScale(1, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
    }

    @Transactional(readOnly = true)
    public List<LeaveBalanceResponse> getLeaveBalances(UUID employeeId) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant context is required");
        }

        List<LeaveType> leaveTypes = leaveTypeRepository.findByCompanyId(tenantId);
        List<LeaveBalanceResponse> responses = new ArrayList<>();

        for (LeaveType type : leaveTypes) {
            List<LeaveLedger> entries = leaveLedgerRepository
                    .findByCompanyIdAndEmployeeIdAndLeaveTypeIdOrderByCreatedAtDesc(tenantId, employeeId, type.getId());

            BigDecimal credited = BigDecimal.ZERO;
            BigDecimal consumed = BigDecimal.ZERO;

            for (LeaveLedger entry : entries) {
                if ("ACCRUAL".equalsIgnoreCase(entry.getTransactionType()) || entry.getDays().compareTo(BigDecimal.ZERO) > 0) {
                    credited = credited.add(entry.getDays());
                } else if ("CONSUMPTION".equalsIgnoreCase(entry.getTransactionType()) || entry.getDays().compareTo(BigDecimal.ZERO) < 0) {
                    consumed = consumed.add(entry.getDays().abs());
                }
            }

            BigDecimal remaining = credited.subtract(consumed).setScale(1, RoundingMode.HALF_UP);

            responses.add(LeaveBalanceResponse.builder()
                    .leaveTypeId(type.getId())
                    .leaveCode(type.getLeaveCode())
                    .leaveName(type.getLeaveName())
                    .creditedDays(credited.setScale(1, RoundingMode.HALF_UP))
                    .consumedDays(consumed.setScale(1, RoundingMode.HALF_UP))
                    .remainingBalance(remaining)
                    .build());
        }

        return responses;
    }

    @Transactional
    public LeaveBalanceResponse creditLeave(UUID employeeId, UUID leaveTypeId, BigDecimal days, String note) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant context is required");
        }

        employeeRepository.findByIdAndCompanyId(employeeId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + employeeId));

        LeaveType leaveType = leaveTypeRepository.findByIdAndCompanyId(leaveTypeId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave type not found with id: " + leaveTypeId));

        if (days == null || days.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Credited days must be greater than zero");
        }

        LeaveLedger ledger = LeaveLedger.builder()
                .employeeId(employeeId)
                .leaveTypeId(leaveTypeId)
                .transactionType("ACCRUAL")
                .days(days.setScale(1, RoundingMode.HALF_UP))
                .referenceNote(note != null ? note : "Leave balance credit")
                .effectiveDate(LocalDate.now())
                .build();
        ledger.setCompanyId(tenantId);

        leaveLedgerRepository.save(ledger);

        BigDecimal balance = getLeaveBalance(employeeId, leaveTypeId);
        return LeaveBalanceResponse.builder()
                .leaveTypeId(leaveType.getId())
                .leaveCode(leaveType.getLeaveCode())
                .leaveName(leaveType.getLeaveName())
                .creditedDays(days.setScale(1, RoundingMode.HALF_UP))
                .consumedDays(BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP))
                .remainingBalance(balance)
                .build();
    }

    @Transactional
    public LeaveApplicationResponse applyLeave(ApplyLeaveRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant context is required");
        }

        UUID employeeId = resolveEmployeeId(request.getEmployeeId(), tenantId);

        Employee employee = employeeRepository.findByIdAndCompanyId(employeeId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + employeeId));

        LeaveType leaveType = leaveTypeRepository.findByIdAndCompanyId(request.getLeaveTypeId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave type not found with id: " + request.getLeaveTypeId()));

        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }

        BigDecimal totalDays = calculateLeaveDays(leaveType, employee.getBranchId(), tenantId, request.getStartDate(), request.getEndDate());

        if (totalDays.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Leave application contains zero working days");
        }

        BigDecimal remainingBalance = getLeaveBalance(employeeId, request.getLeaveTypeId());
        if (totalDays.compareTo(remainingBalance) > 0) {
            throw new IllegalArgumentException("Insufficient leave balance. Available: " + remainingBalance + ", Requested: " + totalDays);
        }

        LeaveApplication application = LeaveApplication.builder()
                .employeeId(employeeId)
                .leaveTypeId(request.getLeaveTypeId())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .totalDays(totalDays)
                .reason(request.getReason())
                .status("PENDING")
                .build();
        application.setCompanyId(tenantId);

        LeaveApplication saved = leaveApplicationRepository.save(application);
        return toApplicationResponse(saved, leaveType);
    }

    @Transactional
    public LeaveApplicationResponse approveLeave(UUID leaveApplicationId) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant context is required");
        }

        LeaveApplication application = leaveApplicationRepository.findByIdAndCompanyId(leaveApplicationId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave application not found with id: " + leaveApplicationId));

        if (!"PENDING".equalsIgnoreCase(application.getStatus())) {
            throw new IllegalStateException("Only PENDING leave applications can be approved");
        }

        LeaveType leaveType = leaveTypeRepository.findByIdAndCompanyId(application.getLeaveTypeId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave type not found with id: " + application.getLeaveTypeId()));

        application.setStatus("APPROVED");
        application.setActionedAt(Instant.now());
        application.setApproverId(tryResolveCurrentUserId());

        LeaveApplication updated = leaveApplicationRepository.save(application);

        // Immutable ledger transaction: CONSUMPTION
        LeaveLedger ledger = LeaveLedger.builder()
                .employeeId(application.getEmployeeId())
                .leaveTypeId(application.getLeaveTypeId())
                .transactionType("CONSUMPTION")
                .days(application.getTotalDays().negate().setScale(1, RoundingMode.HALF_UP))
                .leaveApplicationId(application.getId())
                .referenceNote("Approved leave: " + application.getStartDate() + " to " + application.getEndDate())
                .effectiveDate(application.getStartDate())
                .build();
        ledger.setCompanyId(tenantId);

        leaveLedgerRepository.save(ledger);

        return toApplicationResponse(updated, leaveType);
    }

    @Transactional
    public LeaveApplicationResponse rejectLeave(UUID leaveApplicationId, String reason) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant context is required");
        }

        LeaveApplication application = leaveApplicationRepository.findByIdAndCompanyId(leaveApplicationId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave application not found with id: " + leaveApplicationId));

        if (!"PENDING".equalsIgnoreCase(application.getStatus())) {
            throw new IllegalStateException("Only PENDING leave applications can be rejected");
        }

        LeaveType leaveType = leaveTypeRepository.findByIdAndCompanyId(application.getLeaveTypeId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave type not found with id: " + application.getLeaveTypeId()));

        application.setStatus("REJECTED");
        application.setActionedAt(Instant.now());
        application.setApproverId(tryResolveCurrentUserId());

        LeaveApplication updated = leaveApplicationRepository.save(application);
        return toApplicationResponse(updated, leaveType);
    }

    @Transactional(readOnly = true)
    public List<LeaveApplicationResponse> getLeaveApplications(UUID employeeId, String status) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant context is required");
        }

        List<LeaveApplication> list;
        if (employeeId != null && status != null && !status.isBlank()) {
            list = leaveApplicationRepository.findByCompanyIdAndEmployeeIdAndStatus(tenantId, employeeId, status.toUpperCase());
        } else if (employeeId != null) {
            list = leaveApplicationRepository.findByCompanyIdAndEmployeeId(tenantId, employeeId);
        } else if (status != null && !status.isBlank()) {
            list = leaveApplicationRepository.findByCompanyIdAndStatus(tenantId, status.toUpperCase());
        } else {
            list = leaveApplicationRepository.findAll().stream()
                    .filter(a -> tenantId.equals(a.getCompanyId()))
                    .collect(Collectors.toList());
        }

        return list.stream().map(app -> {
            LeaveType lt = leaveTypeRepository.findByIdAndCompanyId(app.getLeaveTypeId(), tenantId).orElse(null);
            return toApplicationResponse(app, lt);
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public LeaveApplicationResponse getLeaveApplicationById(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant context is required");
        }

        LeaveApplication app = leaveApplicationRepository.findByIdAndCompanyId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave application not found with id: " + id));

        LeaveType lt = leaveTypeRepository.findByIdAndCompanyId(app.getLeaveTypeId(), tenantId).orElse(null);
        return toApplicationResponse(app, lt);
    }

    public BigDecimal calculateLeaveDays(LeaveType leaveType, UUID branchId, UUID tenantId, LocalDate startDate, LocalDate endDate) {
        if (Boolean.TRUE.equals(leaveType.getIsSandwichRuleEnabled())) {
            // Sandwich rule enabled: Count ALL calendar days between startDate and endDate inclusive
            long totalDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;
            return BigDecimal.valueOf(totalDays).setScale(1, RoundingMode.HALF_UP);
        }

        // Sandwich rule disabled: Count only working days, excluding Saturdays, Sundays, and public holidays
        Set<LocalDate> holidays = new HashSet<>();
        if (branchId != null) {
            branchRepository.findByIdAndCompanyId(branchId, tenantId).ifPresent(branch -> {
                List<HolidayCalendar> holidayList = holidayCalendarRepository.findHolidaysBetween(
                        tenantId, branch.getStateCode(), startDate, endDate
                );
                holidayList.forEach(h -> holidays.add(h.getHolidayDate()));
            });
        } else {
            List<HolidayCalendar> holidayList = holidayCalendarRepository.findHolidaysBetween(
                    tenantId, startDate, endDate
            );
            holidayList.forEach(h -> holidays.add(h.getHolidayDate()));
        }

        long workingDays = 0;
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            DayOfWeek dow = date.getDayOfWeek();
            if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
                continue;
            }
            if (holidays.contains(date)) {
                continue;
            }
            workingDays++;
        }

        return BigDecimal.valueOf(workingDays).setScale(1, RoundingMode.HALF_UP);
    }

    public LeaveApplicationResponse toApplicationResponse(LeaveApplication application, LeaveType leaveType) {
        return LeaveApplicationResponse.builder()
                .id(application.getId())
                .companyId(application.getCompanyId())
                .employeeId(application.getEmployeeId())
                .leaveTypeId(application.getLeaveTypeId())
                .leaveCode(leaveType != null ? leaveType.getLeaveCode() : null)
                .leaveName(leaveType != null ? leaveType.getLeaveName() : null)
                .startDate(application.getStartDate())
                .endDate(application.getEndDate())
                .totalDays(application.getTotalDays())
                .reason(application.getReason())
                .status(application.getStatus())
                .approverId(application.getApproverId())
                .actionedAt(application.getActionedAt())
                .createdAt(application.getCreatedAt())
                .build();
    }

    public LeaveTypeResponse toLeaveTypeResponse(LeaveType leaveType) {
        return LeaveTypeResponse.builder()
                .id(leaveType.getId())
                .companyId(leaveType.getCompanyId())
                .leaveName(leaveType.getLeaveName())
                .leaveCode(leaveType.getLeaveCode())
                .annualQuota(leaveType.getAnnualQuota())
                .accrualFrequency(leaveType.getAccrualFrequency())
                .isCarryForwardAllowed(Boolean.TRUE.equals(leaveType.getIsCarryForwardAllowed()))
                .maxCarryForwardLimit(leaveType.getMaxCarryForwardLimit())
                .isSandwichRuleEnabled(Boolean.TRUE.equals(leaveType.getIsSandwichRuleEnabled()))
                .createdAt(leaveType.getCreatedAt())
                .build();
    }

    private UUID resolveEmployeeId(UUID requestEmployeeId, UUID tenantId) {
        if (requestEmployeeId != null) {
            employeeRepository.findByIdAndCompanyId(requestEmployeeId, tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + requestEmployeeId));
            return requestEmployeeId;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName() == null) {
            throw new IllegalArgumentException("Employee ID is required for unauthenticated or anonymous requests");
        }

        String email = auth.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        Employee employee = employeeRepository.findByCompanyIdAndUserId(tenantId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee record not found for user: " + email));

        return employee.getId();
    }

    private UUID tryResolveCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName() == null) {
            return null;
        }

        String email = auth.getName();
        return userRepository.findByEmail(email).map(User::getId).orElse(null);
    }
}

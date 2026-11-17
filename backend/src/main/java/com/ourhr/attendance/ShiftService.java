package com.ourhr.attendance;

import com.ourhr.attendance.dto.CreateShiftRequest;
import com.ourhr.attendance.dto.ShiftResponse;
import com.ourhr.common.ResourceNotFoundException;
import com.ourhr.common.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShiftService {

    private final ShiftRepository shiftRepository;

    @Transactional
    public ShiftResponse createShift(CreateShiftRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant context is required");
        }

        if (request.getShiftName() == null || request.getShiftName().isBlank()) {
            throw new IllegalArgumentException("Shift name cannot be blank");
        }

        shiftRepository.findByCompanyIdAndShiftName(tenantId, request.getShiftName().trim())
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Shift with name '" + request.getShiftName() + "' already exists");
                });

        Shift shift = Shift.builder()
                .shiftName(request.getShiftName().trim())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .graceMinutes(request.getGraceMinutes())
                .halfDayThresholdHours(request.getHalfDayThresholdHours() != null ? request.getHalfDayThresholdHours() : new BigDecimal("4.50"))
                .fullDayThresholdHours(request.getFullDayThresholdHours() != null ? request.getFullDayThresholdHours() : new BigDecimal("8.00"))
                .lateMarkPenaltyCount(request.getLateMarkPenaltyCount())
                .lateMarkPenaltyDeductionDays(request.getLateMarkPenaltyDeductionDays() != null ? request.getLateMarkPenaltyDeductionDays() : new BigDecimal("0.5"))
                .build();
        shift.setCompanyId(tenantId);

        Shift saved = shiftRepository.save(shift);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ShiftResponse> getAllShifts() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant context is required");
        }

        return shiftRepository.findByCompanyId(tenantId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ShiftResponse getShiftById(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant context is required");
        }

        return shiftRepository.findByIdAndCompanyId(id, tenantId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Shift not found with id: " + id));
    }

    public ShiftResponse toResponse(Shift shift) {
        return ShiftResponse.builder()
                .id(shift.getId())
                .companyId(shift.getCompanyId())
                .shiftName(shift.getShiftName())
                .startTime(shift.getStartTime())
                .endTime(shift.getEndTime())
                .graceMinutes(shift.getGraceMinutes())
                .halfDayThresholdHours(shift.getHalfDayThresholdHours())
                .fullDayThresholdHours(shift.getFullDayThresholdHours())
                .lateMarkPenaltyCount(shift.getLateMarkPenaltyCount())
                .lateMarkPenaltyDeductionDays(shift.getLateMarkPenaltyDeductionDays())
                .createdAt(shift.getCreatedAt())
                .build();
    }
}

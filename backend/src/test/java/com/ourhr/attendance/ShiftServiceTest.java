package com.ourhr.attendance;

import com.ourhr.attendance.dto.CreateShiftRequest;
import com.ourhr.attendance.dto.ShiftResponse;
import com.ourhr.common.ResourceNotFoundException;
import com.ourhr.common.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShiftServiceTest {

    @Mock
    private ShiftRepository shiftRepository;

    @InjectMocks
    private ShiftService shiftService;

    private UUID tenantId;
    private Shift sampleShift;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);

        sampleShift = Shift.builder()
                .shiftName("General Shift")
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(18, 0))
                .graceMinutes(15)
                .halfDayThresholdHours(new BigDecimal("4.50"))
                .fullDayThresholdHours(new BigDecimal("8.00"))
                .lateMarkPenaltyCount(3)
                .lateMarkPenaltyDeductionDays(new BigDecimal("0.5"))
                .build();
        sampleShift.setId(UUID.randomUUID());
        sampleShift.setCompanyId(tenantId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // =========================================================================
    // 1. Shift Creation with Grace Minutes & Thresholds
    // =========================================================================

    @Test
    @DisplayName("createShift: successfully creates shift with custom thresholds and grace minutes")
    void createShift_successWithCustomThresholds() {
        CreateShiftRequest request = CreateShiftRequest.builder()
                .shiftName("Morning Shift")
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(17, 0))
                .graceMinutes(10)
                .halfDayThresholdHours(new BigDecimal("4.00"))
                .fullDayThresholdHours(new BigDecimal("8.00"))
                .lateMarkPenaltyCount(3)
                .lateMarkPenaltyDeductionDays(new BigDecimal("0.5"))
                .build();

        when(shiftRepository.findByCompanyIdAndShiftName(tenantId, "Morning Shift")).thenReturn(Optional.empty());
        when(shiftRepository.save(any(Shift.class))).thenAnswer(invocation -> {
            Shift s = invocation.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });

        ShiftResponse response = shiftService.createShift(request);

        assertNotNull(response);
        assertEquals("Morning Shift", response.getShiftName());
        assertEquals(LocalTime.of(8, 0), response.getStartTime());
        assertEquals(LocalTime.of(17, 0), response.getEndTime());
        assertEquals(10, response.getGraceMinutes());
        assertEquals(new BigDecimal("4.00"), response.getHalfDayThresholdHours());
        assertEquals(new BigDecimal("8.00"), response.getFullDayThresholdHours());
        assertEquals(3, response.getLateMarkPenaltyCount());
        assertEquals(new BigDecimal("0.5"), response.getLateMarkPenaltyDeductionDays());

        ArgumentCaptor<Shift> captor = ArgumentCaptor.forClass(Shift.class);
        verify(shiftRepository).save(captor.capture());
        Shift saved = captor.getValue();
        assertEquals(tenantId, saved.getCompanyId());
    }

    @Test
    @DisplayName("createShift: applies default thresholds (4.50 half-day, 8.00 full-day) when omitted")
    void createShift_appliesDefaultThresholds() {
        CreateShiftRequest request = CreateShiftRequest.builder()
                .shiftName("Standard Shift")
                .startTime(LocalTime.of(9, 30))
                .endTime(LocalTime.of(18, 30))
                .graceMinutes(15)
                .build();

        when(shiftRepository.findByCompanyIdAndShiftName(tenantId, "Standard Shift")).thenReturn(Optional.empty());
        when(shiftRepository.save(any(Shift.class))).thenAnswer(invocation -> {
            Shift s = invocation.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });

        ShiftResponse response = shiftService.createShift(request);

        assertNotNull(response);
        assertEquals(new BigDecimal("4.50"), response.getHalfDayThresholdHours());
        assertEquals(new BigDecimal("8.00"), response.getFullDayThresholdHours());
    }

    @Test
    @DisplayName("createShift: throws IllegalArgumentException when duplicate shift name exists in tenant")
    void createShift_throwsWhenDuplicate() {
        CreateShiftRequest request = CreateShiftRequest.builder()
                .shiftName("General Shift")
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(18, 0))
                .build();

        when(shiftRepository.findByCompanyIdAndShiftName(tenantId, "General Shift")).thenReturn(Optional.of(sampleShift));

        assertThrows(IllegalArgumentException.class, () -> shiftService.createShift(request));
    }

    @Test
    @DisplayName("createShift: throws IllegalArgumentException when shift name is blank")
    void createShift_throwsWhenNameBlank() {
        CreateShiftRequest request = CreateShiftRequest.builder()
                .shiftName("   ")
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(18, 0))
                .build();

        assertThrows(IllegalArgumentException.class, () -> shiftService.createShift(request));
    }

    // =========================================================================
    // 2. Query Shifts
    // =========================================================================

    @Test
    @DisplayName("getAllShifts: returns all shifts for current tenant")
    void getAllShifts_returnsList() {
        when(shiftRepository.findByCompanyId(tenantId)).thenReturn(List.of(sampleShift));

        List<ShiftResponse> list = shiftService.getAllShifts();

        assertEquals(1, list.size());
        assertEquals("General Shift", list.get(0).getShiftName());
    }

    @Test
    @DisplayName("getShiftById: returns shift details when found")
    void getShiftById_found() {
        UUID id = sampleShift.getId();
        when(shiftRepository.findByIdAndCompanyId(id, tenantId)).thenReturn(Optional.of(sampleShift));

        ShiftResponse response = shiftService.getShiftById(id);

        assertNotNull(response);
        assertEquals(id, response.getId());
        assertEquals("General Shift", response.getShiftName());
    }

    @Test
    @DisplayName("getShiftById: throws ResourceNotFoundException when not found")
    void getShiftById_notFound() {
        UUID id = UUID.randomUUID();
        when(shiftRepository.findByIdAndCompanyId(id, tenantId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> shiftService.getShiftById(id));
    }

    @Test
    @DisplayName("Throws IllegalArgumentException when tenant context is missing")
    void throwsWhenTenantContextMissing() {
        TenantContext.clear();

        CreateShiftRequest request = CreateShiftRequest.builder().shiftName("S1").build();
        assertThrows(IllegalArgumentException.class, () -> shiftService.createShift(request));
        assertThrows(IllegalArgumentException.class, () -> shiftService.getAllShifts());
        assertThrows(IllegalArgumentException.class, () -> shiftService.getShiftById(UUID.randomUUID()));
    }
}

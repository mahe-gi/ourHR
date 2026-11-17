package com.ourhr.payroll;

import com.ourhr.common.ResourceNotFoundException;
import com.ourhr.common.TenantContext;
import com.ourhr.employee.Employee;
import com.ourhr.employee.EmployeeRepository;
import com.ourhr.employee.kyc.KycVaultService;
import com.ourhr.employee.kyc.dto.KycVaultResponse;
import com.ourhr.payroll.dto.ConsultantPayoutRequest;
import com.ourhr.payroll.dto.ConsultantPayoutResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConsultantPayoutService {

    private final EmployeeRepository employeeRepository;
    private final KycVaultService kycVaultService;

    @Transactional(readOnly = true)
    public ConsultantPayoutResponse processConsultantPayout(ConsultantPayoutRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant context is required");
        }

        Employee employee = employeeRepository.findByIdAndCompanyId(request.getConsultantEmployeeId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + request.getConsultantEmployeeId()));

        String workerType = employee.getWorkerType() != null ? employee.getWorkerType().trim().toUpperCase() : "";
        if (!"CONSULTANT_194J".equalsIgnoreCase(workerType) && !"INTERN".equalsIgnoreCase(workerType)) {
            throw new IllegalArgumentException("Employee worker type must be CONSULTANT_194J or INTERN, found: " + employee.getWorkerType());
        }

        BigDecimal gross = request.getGrossAmount().setScale(2, RoundingMode.HALF_UP);
        BigDecimal rate = request.getTdsRate().setScale(2, RoundingMode.HALF_UP);

        BigDecimal rateFraction = rate.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
        BigDecimal tdsAmount = gross.multiply(rateFraction).setScale(2, RoundingMode.HALF_UP);
        BigDecimal netPayable = gross.subtract(tdsAmount).setScale(2, RoundingMode.HALF_UP);

        String panNumber = null;
        try {
            KycVaultResponse vault = kycVaultService.getKycVault(employee.getId());
            if (vault != null) {
                panNumber = vault.getPanNumber();
            }
        } catch (ResourceNotFoundException ignored) {
            // KYC vault entry may not yet be uploaded
        }

        return ConsultantPayoutResponse.builder()
                .id(UUID.randomUUID())
                .companyId(tenantId)
                .consultantEmployeeId(employee.getId())
                .consultantName(employee.getFirstName() + " " + employee.getLastName())
                .panNumber(panNumber)
                .grossAmount(gross)
                .tdsSection(request.getTdsSection())
                .tdsRate(rate)
                .tdsAmount(tdsAmount)
                .netPayable(netPayable)
                .status("PROCESSED")
                .createdAt(Instant.now())
                .build();
    }
}

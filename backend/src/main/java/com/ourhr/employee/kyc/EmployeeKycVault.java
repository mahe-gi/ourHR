package com.ourhr.employee.kyc;

import com.ourhr.common.BaseTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "employee_kyc_vault")
public class EmployeeKycVault extends BaseTenantEntity {

    @Column(name = "employee_id", nullable = false, unique = true)
    private UUID employeeId;

    @Column(name = "pan_number", length = 10, nullable = false)
    private String panNumber;

    @Builder.Default
    @Column(name = "masked_aadhaar_token", length = 30, nullable = false)
    private String maskedAadhaarToken = "[Aadhaar Redacted]";

    @Column(name = "uan", length = 12)
    private String uan;

    @Column(name = "esic_ip_number", length = 17)
    private String esicIpNumber;

    @Column(name = "bank_account_number", length = 50, nullable = false)
    private String bankAccountNumber;

    @Column(name = "bank_ifsc", length = 11, nullable = false)
    private String bankIfsc;

    @Column(name = "bank_name", length = 100, nullable = false)
    private String bankName;

    @Column(name = "pan_document_url", length = 500)
    private String panDocumentUrl;

    @Column(name = "aadhaar_document_url", length = 500)
    private String aadhaarDocumentUrl;

    @Column(name = "cheque_document_url", length = 500)
    private String chequeDocumentUrl;

    @Column(name = "appointment_letter_url", length = 500)
    private String appointmentLetterUrl;

    @Builder.Default
    @Column(name = "tax_regime_preference", length = 10, nullable = false)
    private String taxRegimePreference = "NEW";
}

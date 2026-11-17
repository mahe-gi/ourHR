package com.ourhr.employee.kyc.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KycVaultResponse {

    private UUID id;
    private UUID companyId;
    private UUID employeeId;
    private String panNumber;
    private String maskedAadhaarToken;
    private String uan;
    private String esicIpNumber;
    private String bankAccountNumberMasked;
    private String bankIfsc;
    private String bankName;
    private String panDocumentUrl;
    private String aadhaarDocumentUrl;
    private String chequeDocumentUrl;
    private String appointmentLetterUrl;
    private String taxRegimePreference;
    private Instant createdAt;
    private Instant updatedAt;
}

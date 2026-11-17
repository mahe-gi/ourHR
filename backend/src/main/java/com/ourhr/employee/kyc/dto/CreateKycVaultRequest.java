package com.ourhr.employee.kyc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateKycVaultRequest {

    private UUID employeeId;

    @NotBlank(message = "PAN number is required")
    @Pattern(regexp = "[A-Z]{5}[0-9]{4}[A-Z]{1}", message = "PAN must follow valid format ABCDE1234F")
    private String panNumber;

    @Pattern(regexp = "^[0-9]{12}$", message = "Aadhaar must be 12 digits")
    private String aadhaarNumber;

    @Pattern(regexp = "^[0-9]{12}$", message = "UAN must be 12 digits")
    private String uan;

    @Pattern(regexp = "^[0-9]{17}$", message = "ESIC IP must be 17 digits")
    private String esicIpNumber;

    @NotBlank(message = "Bank account number is required")
    @Size(min = 8, max = 30, message = "Bank account number must be between 8 and 30 digits")
    private String bankAccountNumber;

    @NotBlank(message = "Bank IFSC is required")
    @Pattern(regexp = "^[A-Z]{4}0[A-Z0-9]{6}$", message = "Bank IFSC must follow standard RBI format ABCD0123456")
    private String bankIfsc;

    @NotBlank(message = "Bank name is required")
    @Size(max = 100, message = "Bank name cannot exceed 100 characters")
    private String bankName;

    private String panDocumentUrl;
    private String aadhaarDocumentUrl;
    private String chequeDocumentUrl;
    private String appointmentLetterUrl;

    @Builder.Default
    private String taxRegimePreference = "NEW";
}

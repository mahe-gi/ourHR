package com.ourhr.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterCompanyRequest {

    @NotBlank(message = "Company legal name is required")
    private String legalName;

    private String tradeName;

    @NotBlank(message = "PAN number is required")
    @Pattern(regexp = "^[A-Z]{5}[0-9]{4}[A-Z]{1}$", message = "Invalid PAN number format (e.g. ABCDE1234F)")
    private String panNumber;

    @NotBlank(message = "TAN number is required")
    @Pattern(regexp = "^[A-Z]{4}[0-9]{5}[A-Z]{1}$", message = "Invalid TAN number format (e.g. ABCD12345E)")
    private String tanNumber;

    private String gstin;

    @NotBlank(message = "Signatory name is required")
    private String signatoryName;

    @NotBlank(message = "Signatory designation is required")
    private String signatoryDesignation;

    @NotBlank(message = "State code is required for headquarters branch")
    @Size(min = 2, max = 2, message = "State code must be 2 characters (e.g. KA, MH, DL)")
    private String stateCode;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "Address line is required")
    private String addressLine;

    @NotBlank(message = "Pincode is required")
    @Pattern(regexp = "^[1-9][0-9]{5}$", message = "Invalid 6-digit Indian pincode")
    private String pincode;

    @NotBlank(message = "Admin email is required")
    @Email(message = "Invalid email format")
    private String adminEmail;

    @NotBlank(message = "Admin password is required")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    private String adminPassword;

    @NotBlank(message = "Admin full name is required")
    private String adminFullName;

    private String adminPhoneNumber;
}

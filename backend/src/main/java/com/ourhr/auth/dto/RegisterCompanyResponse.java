package com.ourhr.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterCompanyResponse {

    private UUID companyId;
    private String legalName;
    private UUID adminUserId;
    private String adminEmail;
    private String adminFullName;
    private String message;
}

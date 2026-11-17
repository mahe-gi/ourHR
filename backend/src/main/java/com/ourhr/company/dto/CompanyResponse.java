package com.ourhr.company.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyResponse {

    private UUID id;
    private String legalName;
    private String tradeName;
    private String panNumber;
    private String tanNumber;
    private String gstin;
    private String pfEstablishmentCode;
    private String esicCode;
    private String lin;
    private String signatoryName;
    private String signatoryDesignation;
    private String signatorySignatureUrl;
}

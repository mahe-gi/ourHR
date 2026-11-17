package com.ourhr.company.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCompanyRequest {

    private String tradeName;
    private String gstin;
    private String pfEstablishmentCode;
    private String esicCode;
    private String lin;
    private String signatoryName;
    private String signatoryDesignation;
    private String signatorySignatureUrl;
}

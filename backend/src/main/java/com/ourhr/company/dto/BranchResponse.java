package com.ourhr.company.dto;

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
public class BranchResponse {

    private UUID id;
    private UUID companyId;
    private String branchName;
    private String stateCode;
    private String city;
    private String addressLine;
    private String pincode;
    private Boolean isHeadquarters;
    private Instant createdAt;
}

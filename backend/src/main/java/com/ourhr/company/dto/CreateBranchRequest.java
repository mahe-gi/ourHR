package com.ourhr.company.dto;

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
public class CreateBranchRequest {

    @NotBlank(message = "Branch name is required")
    private String branchName;

    @NotBlank(message = "State code is required")
    @Size(min = 2, max = 2, message = "State code must be 2 characters (e.g. KA, MH, DL, TG)")
    private String stateCode;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "Address line is required")
    private String addressLine;

    @NotBlank(message = "Pincode is required")
    @Pattern(regexp = "^[1-9][0-9]{5}$", message = "Invalid 6-digit Indian pincode")
    private String pincode;

    @Builder.Default
    private Boolean isHeadquarters = false;
}

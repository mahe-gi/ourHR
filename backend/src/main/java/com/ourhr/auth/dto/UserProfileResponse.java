package com.ourhr.auth.dto;

import com.ourhr.security.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {

    private UUID userId;
    private UUID companyId;
    private String companyName;
    private String email;
    private String fullName;
    private String phoneNumber;
    private List<Role> roles;
    private boolean isActive;
}

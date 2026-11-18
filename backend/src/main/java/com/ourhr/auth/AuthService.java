package com.ourhr.auth;

import com.ourhr.auth.dto.AuthResponse;
import com.ourhr.auth.dto.LoginRequest;
import com.ourhr.auth.dto.RegisterCompanyRequest;
import com.ourhr.auth.dto.RegisterCompanyResponse;
import com.ourhr.auth.dto.UserProfileResponse;
import com.ourhr.common.AuditLog;
import com.ourhr.common.AuditLogRepository;
import com.ourhr.common.TenantContext;
import com.ourhr.company.Branch;
import com.ourhr.company.BranchRepository;
import com.ourhr.company.Company;
import com.ourhr.company.CompanyRepository;
import com.ourhr.security.JwtService;
import com.ourhr.security.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final CompanyRepository companyRepository;
    private final BranchRepository branchRepository;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public RegisterCompanyResponse registerCompany(RegisterCompanyRequest request) {
        if (companyRepository.existsByPanNumber(request.getPanNumber())) {
            throw new IllegalArgumentException("Company with PAN " + request.getPanNumber() + " is already registered");
        }
        if (companyRepository.existsByTanNumber(request.getTanNumber())) {
            throw new IllegalArgumentException("Company with TAN " + request.getTanNumber() + " is already registered");
        }
        if (userRepository.existsByEmail(request.getAdminEmail())) {
            throw new IllegalArgumentException("User with email " + request.getAdminEmail() + " already exists");
        }

        Company company = Company.builder()
                .legalName(request.getLegalName())
                .tradeName(request.getTradeName())
                .panNumber(request.getPanNumber().toUpperCase())
                .tanNumber(request.getTanNumber().toUpperCase())
                .gstin(request.getGstin())
                .signatoryName(request.getSignatoryName())
                .signatoryDesignation(request.getSignatoryDesignation())
                .build();
        Company savedCompany = companyRepository.save(company);

        TenantContext.setTenantId(savedCompany.getId());
        try {
            Branch hqBranch = Branch.builder()
                    .branchName("Headquarters")
                    .stateCode(request.getStateCode().toUpperCase())
                    .city(request.getCity())
                    .addressLine(request.getAddressLine())
                    .pincode(request.getPincode())
                    .isHeadquarters(true)
                    .build();
            hqBranch.setCompanyId(savedCompany.getId());
            branchRepository.save(hqBranch);

            User adminUser = User.builder()
                    .email(request.getAdminEmail().toLowerCase())
                    .passwordHash(passwordEncoder.encode(request.getAdminPassword()))
                    .fullName(request.getAdminFullName())
                    .phoneNumber(request.getAdminPhoneNumber())
                    .isActive(true)
                    .build();
            adminUser.setCompanyId(savedCompany.getId());

            UserRole founderRole = UserRole.builder()
                    .roleName(Role.ROLE_FOUNDER)
                    .build();
            UserRole hrRole = UserRole.builder()
                    .roleName(Role.ROLE_HR)
                    .build();

            adminUser.addRole(founderRole);
            adminUser.addRole(hrRole);

            User savedUser = userRepository.save(adminUser);

            AuditLog auditLog = AuditLog.builder()
                    .companyId(savedCompany.getId())
                    .actorId(savedUser.getId())
                    .role("ROLE_FOUNDER")
                    .action("COMPANY_REGISTERED")
                    .entityName("Company")
                    .entityId(savedCompany.getId())
                    .afterJson("{\"legalName\":\"" + savedCompany.getLegalName() + "\",\"pan\":\"" + savedCompany.getPanNumber() + "\"}")
                    .build();
            auditLogRepository.save(auditLog);

            return RegisterCompanyResponse.builder()
                    .companyId(savedCompany.getId())
                    .legalName(savedCompany.getLegalName())
                    .adminUserId(savedUser.getId())
                    .adminEmail(savedUser.getEmail())
                    .adminFullName(savedUser.getFullName())
                    .message("Company and administrator successfully registered")
                    .build();
        } finally {
            TenantContext.clear();
        }
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new BadCredentialsException("User account is inactive. Please contact your company administrator.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        List<Role> roles = user.getRoles().stream()
                .map(UserRole::getRoleName)
                .collect(Collectors.toList());

        List<String> roleNames = roles.stream()
                .map(Enum::name)
                .collect(Collectors.toList());

        String accessToken = jwtService.generateAccessToken(
                user.getId(),
                user.getCompanyId(),
                user.getEmail(),
                roleNames
        );

        String companyName = companyRepository.findById(user.getCompanyId())
                .map(Company::getLegalName)
                .orElse("Unknown Company");

        return AuthResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresInMs(jwtService.getExpirationMs())
                .userId(user.getId())
                .companyId(user.getCompanyId())
                .companyName(companyName)
                .email(user.getEmail())
                .fullName(user.getFullName())
                .roles(roles)
                .build();
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUser(String email) {
        User user = userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String companyName = companyRepository.findById(user.getCompanyId())
                .map(Company::getLegalName)
                .orElse("Unknown Company");

        List<Role> roles = user.getRoles().stream()
                .map(UserRole::getRoleName)
                .collect(Collectors.toList());

        return UserProfileResponse.builder()
                .userId(user.getId())
                .companyId(user.getCompanyId())
                .companyName(companyName)
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .roles(roles)
                .isActive(user.getIsActive())
                .build();
    }
}

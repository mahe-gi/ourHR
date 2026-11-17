package com.ourhr.auth;

import com.ourhr.auth.dto.AuthResponse;
import com.ourhr.auth.dto.LoginRequest;
import com.ourhr.auth.dto.RegisterCompanyRequest;
import com.ourhr.auth.dto.RegisterCompanyResponse;
import com.ourhr.common.AuditLogRepository;
import com.ourhr.company.Branch;
import com.ourhr.company.BranchRepository;
import com.ourhr.company.Company;
import com.ourhr.company.CompanyRepository;
import com.ourhr.security.JwtService;
import com.ourhr.security.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private BranchRepository branchRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private RegisterCompanyRequest registerRequest;
    private Company company;
    private User user;

    @BeforeEach
    void setUp() {
        registerRequest = RegisterCompanyRequest.builder()
                .legalName("Acme Technologies Pvt Ltd")
                .panNumber("ABCDE1234F")
                .tanNumber("BLRA12345E")
                .signatoryName("Rahul Sharma")
                .signatoryDesignation("Managing Director")
                .stateCode("KA")
                .city("Bangalore")
                .addressLine("100 Feet Road, Indiranagar")
                .pincode("560038")
                .adminEmail("founder@acme.com")
                .adminPassword("SecurePass123")
                .adminFullName("Rahul Sharma")
                .build();

        UUID companyId = UUID.randomUUID();
        company = Company.builder()
                .id(companyId)
                .legalName("Acme Technologies Pvt Ltd")
                .panNumber("ABCDE1234F")
                .tanNumber("BLRA12345E")
                .signatoryName("Rahul Sharma")
                .signatoryDesignation("Managing Director")
                .build();

        user = User.builder()
                .email("founder@acme.com")
                .passwordHash("hashedPassword")
                .fullName("Rahul Sharma")
                .isActive(true)
                .build();
        user.setId(UUID.randomUUID());
        user.setCompanyId(companyId);

        UserRole role = UserRole.builder()
                .id(UUID.randomUUID())
                .user(user)
                .roleName(Role.ROLE_FOUNDER)
                .build();
        user.setRoles(Set.of(role));
    }

    @Test
    @DisplayName("Should successfully register company, HQ branch, and founder user")
    void shouldRegisterCompanySuccessfully() {
        when(companyRepository.existsByPanNumber("ABCDE1234F")).thenReturn(false);
        when(companyRepository.existsByTanNumber("BLRA12345E")).thenReturn(false);
        when(userRepository.existsByEmail("founder@acme.com")).thenReturn(false);
        when(passwordEncoder.encode("SecurePass123")).thenReturn("hashedPassword");
        when(companyRepository.save(any(Company.class))).thenReturn(company);
        when(branchRepository.save(any(Branch.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.save(any(User.class))).thenReturn(user);

        RegisterCompanyResponse response = authService.registerCompany(registerRequest);

        assertNotNull(response);
        assertEquals(company.getId(), response.getCompanyId());
        assertEquals("Acme Technologies Pvt Ltd", response.getLegalName());
        assertEquals("founder@acme.com", response.getAdminEmail());

        verify(companyRepository).save(any(Company.class));
        verify(branchRepository).save(any(Branch.class));
        verify(userRepository).save(any(User.class));
        verify(auditLogRepository).save(any());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when registering duplicate PAN")
    void shouldRejectDuplicatePan() {
        when(companyRepository.existsByPanNumber("ABCDE1234F")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> authService.registerCompany(registerRequest));
    }

    @Test
    @DisplayName("Should authenticate valid credentials and issue JWT token")
    void shouldLoginSuccessfully() {
        LoginRequest loginRequest = LoginRequest.builder()
                .email("founder@acme.com")
                .password("SecurePass123")
                .build();

        when(userRepository.findByEmail("founder@acme.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("SecurePass123", "hashedPassword")).thenReturn(true);
        when(jwtService.generateAccessToken(any(), any(), any(), any())).thenReturn("mock.jwt.token");
        when(jwtService.getExpirationMs()).thenReturn(86400000L);
        when(companyRepository.findById(user.getCompanyId())).thenReturn(Optional.of(company));

        AuthResponse authResponse = authService.login(loginRequest);

        assertNotNull(authResponse);
        assertEquals("mock.jwt.token", authResponse.getAccessToken());
        assertEquals("Bearer", authResponse.getTokenType());
        assertEquals(user.getId(), authResponse.getUserId());
        assertEquals(company.getId(), authResponse.getCompanyId());
    }

    @Test
    @DisplayName("Should throw BadCredentialsException on invalid password")
    void shouldRejectInvalidPassword() {
        LoginRequest loginRequest = LoginRequest.builder()
                .email("founder@acme.com")
                .password("WrongPassword")
                .build();

        when(userRepository.findByEmail("founder@acme.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPassword", "hashedPassword")).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> authService.login(loginRequest));
    }

    @Test
    @DisplayName("Should throw BadCredentialsException when user is inactive")
    void shouldRejectInactiveUser() {
        user.setIsActive(false);
        LoginRequest loginRequest = LoginRequest.builder()
                .email("founder@acme.com")
                .password("SecurePass123")
                .build();

        when(userRepository.findByEmail("founder@acme.com")).thenReturn(Optional.of(user));

        assertThrows(BadCredentialsException.class, () -> authService.login(loginRequest));
    }
}

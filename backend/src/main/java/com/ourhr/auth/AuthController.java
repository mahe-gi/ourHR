package com.ourhr.auth;

import com.ourhr.auth.dto.AuthResponse;
import com.ourhr.auth.dto.LoginRequest;
import com.ourhr.auth.dto.RegisterCompanyRequest;
import com.ourhr.auth.dto.RegisterCompanyResponse;
import com.ourhr.auth.dto.UserProfileResponse;
import com.ourhr.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Tenant registration and authentication endpoints")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register-company")
    @Operation(summary = "Register a new tenant company and founder administrator")
    public ResponseEntity<ApiResponse<RegisterCompanyResponse>> registerCompany(
            @Valid @RequestBody RegisterCompanyRequest request
    ) {
        RegisterCompanyResponse response = authService.registerCompany(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user and issue JWT access token")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request
    ) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/me")
    @Operation(summary = "Retrieve current authenticated user profile and tenant details",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<UserProfileResponse>> getCurrentUser(Authentication authentication) {
        UserProfileResponse response = authService.getCurrentUser(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

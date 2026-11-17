package com.ourhr.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private JwtService jwtService;
    private final String secretKey = "dGhpc2lzYXZlcnlsb25nYW5kc2VjdXJlMjU2Yml0c2VjcmV0a2V5Zm9yb3VyaHJhcHAxMjM0NTY3ODkwMTI=";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", secretKey);
        ReflectionTestUtils.setField(jwtService, "jwtExpirationMs", 3600000L); // 1 hour
        ReflectionTestUtils.setField(jwtService, "refreshExpirationMs", 86400000L); // 24 hours
    }

    @Test
    @DisplayName("Should generate and validate JWT access token with all custom claims")
    void shouldGenerateAndValidateAccessToken() {
        UUID userId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        String email = "founder@acme.com";
        List<String> roles = List.of("ROLE_FOUNDER", "ROLE_HR");

        String token = jwtService.generateAccessToken(userId, companyId, email, roles);

        assertNotNull(token);
        assertTrue(jwtService.isTokenValid(token));
        assertEquals(email, jwtService.extractEmail(token));
        assertEquals(userId, jwtService.extractUserId(token));
        assertEquals(companyId, jwtService.extractCompanyId(token));
        assertEquals(roles, jwtService.extractRoles(token));
    }

    @Test
    @DisplayName("Should reject invalid or tampered JWT token")
    void shouldRejectInvalidToken() {
        String invalidToken = "invalid.tampered.token";
        assertFalse(jwtService.isTokenValid(invalidToken));
    }
}

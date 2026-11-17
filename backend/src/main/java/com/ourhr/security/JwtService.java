package com.ourhr.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${ourhr.security.jwt.secret}")
    private String secretKey;

    @Value("${ourhr.security.jwt.expiration-ms:86400000}")
    private long jwtExpirationMs;

    @Value("${ourhr.security.jwt.refresh-expiration-ms:604800000}")
    private long refreshExpirationMs;

    public String generateAccessToken(UUID userId, UUID companyId, String email, List<String> roles) {
        Map<String, Object> extraClaims = Map.of(
                "userId", userId.toString(),
                "companyId", companyId.toString(),
                "roles", roles
        );
        return buildToken(extraClaims, email, jwtExpirationMs);
    }

    public String generateRefreshToken(UUID userId, UUID companyId, String email) {
        Map<String, Object> extraClaims = Map.of(
                "userId", userId.toString(),
                "companyId", companyId.toString()
        );
        return buildToken(extraClaims, email, refreshExpirationMs);
    }

    public String generateOnboardingToken(UUID employeeId, UUID companyId, long expirationMs) {
        Map<String, Object> extraClaims = Map.of(
                "employeeId", employeeId.toString(),
                "companyId", companyId.toString(),
                "type", "ONBOARDING"
        );
        return buildToken(extraClaims, employeeId.toString(), expirationMs);
    }

    public UUID extractEmployeeId(String token) {
        String empIdStr = extractClaim(token, claims -> claims.get("employeeId", String.class));
        return empIdStr != null ? UUID.fromString(empIdStr) : null;
    }

    private String buildToken(Map<String, Object> extraClaims, String subject, long expiration) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return !claims.getExpiration().before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public UUID extractUserId(String token) {
        String userIdStr = extractClaim(token, claims -> claims.get("userId", String.class));
        return userIdStr != null ? UUID.fromString(userIdStr) : null;
    }

    public UUID extractCompanyId(String token) {
        String companyIdStr = extractClaim(token, claims -> claims.get("companyId", String.class));
        return companyIdStr != null ? UUID.fromString(companyIdStr) : null;
    }

    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        return extractClaim(token, claims -> claims.get("roles", List.class));
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public long getExpirationMs() {
        return jwtExpirationMs;
    }
}

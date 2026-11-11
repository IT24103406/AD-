package com.ridelink.ridemanagementservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Service for validating JWT tokens issued by the Account Service.
 *
 * This service does NOT generate tokens. It only validates them.
 * The JWT_SECRET must match exactly what the Account Service uses.
 *
 * Claim names ("role", "sub", "id") must match Account Service's token generation.
 * Adjust the claim names in extractXxx() methods if Account Service uses different keys.
 */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    @Value("${jwt.secret}")
    private String jwtSecret;

    /**
     * Build the signing key from the configured secret.
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(jwtSecret);
        } catch (Exception e) {
            keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Extract all claims from a JWT token.
     *
     * @throws JwtException if the token is invalid, expired, or tampered
     */
    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Validate the JWT signature and expiration.
     */
    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return !claims.getExpiration().before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Extract the subject (typically email) from the token.
     * Account Service typically sets the email as 'sub'.
     */
    public String extractSubject(String token) {
        return extractAllClaims(token).getSubject();
    }

    /**
     * Extract the account ID from the token.
     *
     * IMPORTANT: Adjust the claim key "id" to match Account Service's actual claim name.
     * Common alternatives: "userId", "accountId", "sub" (if sub is the ID).
     */
    public String extractAccountId(String token) {
        Claims claims = extractAllClaims(token);
        Object id = claims.get("id");
        if (id == null) {
            // Fallback: some services use the subject as the ID
            return claims.getSubject();
        }
        return id.toString();
    }

    /**
     * Extract the role from the token.
     *
     * IMPORTANT: Adjust the claim key "role" to match Account Service's actual claim name.
     * Common alternatives: "roles", "authorities", "ROLE".
     */
    public String extractRole(String token) {
        Claims claims = extractAllClaims(token);
        Object role = claims.get("role");
        return role != null ? role.toString() : null;
    }
}

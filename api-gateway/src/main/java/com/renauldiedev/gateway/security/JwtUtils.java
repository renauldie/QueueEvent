package com.renauldiedev.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class JwtUtils {

    private final SecretKey signingKey;

    public JwtUtils(@Value("${jwt.secret}") String secret) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT token has expired: {}", e.getMessage());
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
        }
        return false;
    }

    public Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String getUserId(String token) {
        Claims claims = getClaims(token);
        Object userId = claims.get("userId");
        return userId != null ? String.valueOf(userId) : claims.getSubject();
    }

    public String getRole(String token) {
        Claims claims = getClaims(token);
        Object role = claims.get("role");
        return role != null ? String.valueOf(role) : "ROLE_USER";
    }

    public String getEmail(String token) {
        Claims claims = getClaims(token);
        Object email = claims.get("email");
        return email != null ? String.valueOf(email) : claims.getSubject();
    }
}

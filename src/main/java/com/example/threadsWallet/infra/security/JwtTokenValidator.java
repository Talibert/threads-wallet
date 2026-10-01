package com.example.api_docker.infra.security;

import com.example.api_docker.domain.user.UserId;
import com.example.api_docker.domain.user.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Valida o token do usuário
 */
@Component
public class JwtTokenValidator {

    private final JwtSecretKey jwtSecretKey;

    public JwtTokenValidator(JwtSecretKey jwtSecretKey) {
        this.jwtSecretKey = jwtSecretKey;
    }

    public Claims validate(String token) {
        return Jwts.parser()
                .verifyWith(jwtSecretKey.get())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public UserId extractUserId(Claims claims) {
        return new UserId(UUID.fromString(claims.getSubject()));
    }

    public UserRole extractRole(Claims claims) {
        String role = claims.get("role", String.class);
        return UserRole.valueOf(role);
    }
}

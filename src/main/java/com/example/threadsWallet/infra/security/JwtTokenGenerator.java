package com.example.api_docker.infra.security;

import com.example.api_docker.domain.user.Email;
import com.example.api_docker.domain.user.TokenGenerator;
import com.example.api_docker.domain.user.UserId;
import com.example.api_docker.domain.user.UserRole;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Date;

/**
 * Gera o token do usuário para utilizar em requisições fechadas
 */
@Component
public class JwtTokenGenerator implements TokenGenerator {

    private final JwtSecretKey jwtSecretKey;
    private final long expirationMillis;

    public JwtTokenGenerator(
            JwtSecretKey jwtSecretKey,
            @Value("${jwt.expiration:86400000}") long expirationMillis) {
        this.jwtSecretKey = jwtSecretKey;
        this.expirationMillis = expirationMillis;
    }

    @Override
    public String generate(UserId userId, Email email, UserRole userRole) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.value().toString())
                .claim("email", email.value())
                .claim("role", userRole.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMillis)))
                .signWith(jwtSecretKey.get())
                .compact();
    }
}

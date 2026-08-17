package com.truckassist.backend.service;

import com.truckassist.backend.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey secretKey;
    private final long expiryMinutes;

    public JwtService(
            @Value("${truckassist.auth.jwt.secret}")
            String secret,

            @Value("${truckassist.auth.jwt.expiry-minutes:60}")
            long expiryMinutes) {

        if (secret == null || secret.length() < 32) {
            throw new IllegalArgumentException(
                    "JWT_SECRET must contain at least 32 characters"
            );
        }

        this.secretKey = Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8)
        );

        this.expiryMinutes = expiryMinutes;
    }

    public String generateToken(User user) {

        Instant now = Instant.now();

        Instant expiry = now.plus(
                Duration.ofMinutes(expiryMinutes)
        );

        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("phone", user.getPhone())
                .claim("role", user.getRole())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(secretKey)
                .compact();
    }

    public Claims parseToken(String token) {

        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isValid(String token) {

        try {
            parseToken(token);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public long getExpirySeconds() {

        return Duration.ofMinutes(expiryMinutes)
                .toSeconds();
    }
}
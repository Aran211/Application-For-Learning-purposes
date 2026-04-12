package com.example.ronproject.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ronproject.config.JwtProperties;
import com.example.ronproject.user.CurrentUser;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private final JwtProperties jwtProperties;
    private final SecretKey signingKey;
    private final RevokedTokenRepository revokedTokenRepository;

    public JwtService(JwtProperties jwtProperties, RevokedTokenRepository revokedTokenRepository) {
        this.jwtProperties = jwtProperties;
        this.signingKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
        this.revokedTokenRepository = revokedTokenRepository;
    }

    public String generateToken(CurrentUser currentUser) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(jwtProperties.getExpiration());
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(currentUser.getUsername())
                .claim("uid", currentUser.getId().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey)
                .compact();
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public boolean isValid(String token, CurrentUser currentUser) {
        Claims claims = parseClaims(token);
        return currentUser.getUsername().equals(claims.getSubject())
                && claims.getExpiration().after(new Date())
                && !isRevoked(claims.getId());
    }

    @Transactional
    public void revokeToken(String token) {
        Claims claims = parseClaims(token);
        String jti = claims.getId();
        if (jti != null && !revokedTokenRepository.existsByTokenId(jti)) {
            UUID userId = UUID.fromString(claims.get("uid", String.class));
            Instant expiresAt = claims.getExpiration().toInstant();
            revokedTokenRepository.save(new RevokedToken(jti, userId, expiresAt));
            log.info("Token revoked: jti={}, userId={}", jti, userId);
        }
    }

    public boolean isRevoked(String jti) {
        return jti != null && revokedTokenRepository.existsByTokenId(jti);
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}

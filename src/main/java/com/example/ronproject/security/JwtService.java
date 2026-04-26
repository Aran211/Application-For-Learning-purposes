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
    private final IssuedTokenRepository issuedTokenRepository;

    public JwtService(JwtProperties jwtProperties, IssuedTokenRepository issuedTokenRepository) {
        this.jwtProperties = jwtProperties;
        this.signingKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
        this.issuedTokenRepository = issuedTokenRepository;
    }

    @Transactional
    public String generateToken(CurrentUser currentUser) {
        String jti = UUID.randomUUID().toString();
        Instant now = Instant.now();
        Instant expiresAt = now.plus(jwtProperties.getExpiration());
        String token = Jwts.builder()
                .id(jti)
                .subject(currentUser.getUsername())
                .claim("uid", currentUser.getId().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey)
                .compact();
        issuedTokenRepository.save(new IssuedToken(jti, currentUser.getId(), now, expiresAt));
        return token;
    }

    public boolean isValid(Claims claims, CurrentUser currentUser) {
        return currentUser.getUsername().equals(claims.getSubject())
                && claims.getExpiration().after(new Date())
                && !isRevoked(claims.getId());
    }

    @Transactional
    public void revokeToken(String token) {
        Claims claims = parseClaims(token);
        String jti = claims.getId();
        if (jti == null) return;
        issuedTokenRepository.findByTokenId(jti).ifPresent(issued -> {
            if (!issued.isRevoked()) {
                issued.revoke();
                log.info("Token revoked: jti={}, userId={}", jti, issued.getUserId());
            }
        });
    }

    public boolean isRevoked(String jti) {
        if (jti == null) return true;
        return issuedTokenRepository.findByTokenId(jti)
                .map(IssuedToken::isRevoked)
                .orElse(true);
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}

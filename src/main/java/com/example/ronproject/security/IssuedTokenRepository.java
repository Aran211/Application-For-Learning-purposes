package com.example.ronproject.security;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface IssuedTokenRepository extends JpaRepository<IssuedToken, UUID> {

    Optional<IssuedToken> findByTokenId(String tokenId);

    @Modifying
    @Query("DELETE FROM IssuedToken t WHERE t.expiresAt < :now")
    int deleteExpiredTokens(Instant now);
}

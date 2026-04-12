package com.example.ronproject.auth;

import java.util.UUID;

public record AuthResponse(
        String token,
        UUID userId,
        String username,
        String email
) {
}

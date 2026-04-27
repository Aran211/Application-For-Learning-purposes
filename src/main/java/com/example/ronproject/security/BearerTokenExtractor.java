package com.example.ronproject.security;

public final class BearerTokenExtractor {

    private static final String BEARER_PREFIX = "Bearer ";

    private BearerTokenExtractor() {}

    public static boolean hasBearerToken(String authHeader) {
        return authHeader != null && authHeader.startsWith(BEARER_PREFIX);
    }

    public static String extract(String authHeader) {
        return authHeader.substring(BEARER_PREFIX.length());
    }
}

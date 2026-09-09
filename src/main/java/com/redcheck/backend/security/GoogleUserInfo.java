package com.redcheck.backend.security;

public record GoogleUserInfo(
        String googleId,
        String email,
        boolean emailVerified,
        String name
) {}

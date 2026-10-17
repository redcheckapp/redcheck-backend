package com.redcheck.backend.dto.response;

import lombok.Builder;

@Builder
public record UserResponseDTO(
        String username,
        String alias,
        String email,
        boolean hasPassword,
        // Whether to show the developer dashboard entry point. Cosmetic only —
        // /admin/** is enforced server-side regardless (see SecurityConfig).
        boolean isAdmin
) {}
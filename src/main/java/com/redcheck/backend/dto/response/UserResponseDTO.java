package com.redcheck.backend.dto.response;

import lombok.Builder;

@Builder
public record UserResponseDTO(
        String username,
        String alias,
        String email,
        boolean hasPassword
) {}
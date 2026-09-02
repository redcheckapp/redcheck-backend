package com.redcheck.backend.dto.request;

import lombok.Builder;

@Builder
public record RegisterRequestDTO(
        String username,
        String email,
        String password
) {}
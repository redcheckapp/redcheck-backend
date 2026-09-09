package com.redcheck.backend.dto.request;

import lombok.Builder;

@Builder
public record LoginRequestDTO(
        String emailOrUsername,
        String password
) {}
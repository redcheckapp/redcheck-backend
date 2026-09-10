package com.redcheck.backend.dto.request;

import lombok.Builder;

@Builder
public record ForgotPasswordRequestDTO(
        String email,
        String lang
) {}

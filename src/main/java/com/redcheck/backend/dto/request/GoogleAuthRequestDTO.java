package com.redcheck.backend.dto.request;

import lombok.Builder;

@Builder
public record GoogleAuthRequestDTO(
        String idToken
) {}

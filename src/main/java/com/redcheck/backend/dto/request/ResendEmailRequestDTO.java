package com.redcheck.backend.dto.request;

import lombok.Builder;

@Builder
public record ResendEmailRequestDTO(
        String from,
        String to,
        String subject,
        String html
) {}

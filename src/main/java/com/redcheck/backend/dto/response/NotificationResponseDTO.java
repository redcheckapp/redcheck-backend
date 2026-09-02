package com.redcheck.backend.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record NotificationResponseDTO(
        Long id,
        String title,
        String message,
        boolean read,
        LocalDateTime creationDate
) {}
package com.redcheck.backend.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record RecurringTaskResponseDTO(
        Long id,
        String title,
        String description,
        String frequency,
        boolean active,
        LocalDateTime createdDate,
        LocalDateTime latestGeneratedDate,
        Long subjectId
) {}
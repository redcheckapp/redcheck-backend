package com.redcheck.backend.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record TaskResponseDTO(
        Long id,
        String title,
        String description,
        LocalDateTime assignedDate,
        LocalDateTime deadline,
        LocalDateTime completedDate,
        boolean deleted,
        boolean completed,
        boolean overdue,
        Long subjectId
) {}
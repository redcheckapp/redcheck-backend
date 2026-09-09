package com.redcheck.backend.dto.response;

import com.redcheck.backend.entity.Feedback;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record FeedbackResponseDTO(
        Long id,
        Feedback.Category category,
        String message,
        LocalDateTime createdDate
) {}

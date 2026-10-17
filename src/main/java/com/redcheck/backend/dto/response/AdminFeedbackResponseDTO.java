package com.redcheck.backend.dto.response;

import com.redcheck.backend.entity.Feedback;
import lombok.Builder;

import java.time.LocalDateTime;

// Developer-dashboard view of a Feedback row. userEmail/userAlias are null
// when the author has since deleted their account (Feedback.user is SET NULL).
@Builder
public record AdminFeedbackResponseDTO(
        Long id,
        Feedback.Category category,
        Feedback.Status status,
        String message,
        String adminNote,
        LocalDateTime createdDate,
        LocalDateTime userNotifiedDate,
        String userEmail,
        String userAlias
) {}

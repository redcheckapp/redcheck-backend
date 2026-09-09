package com.redcheck.backend.dto.request;

import com.redcheck.backend.entity.Feedback;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record FeedbackRequestDTO(
        @NotNull(message = "Category is required")
        Feedback.Category category,

        @NotBlank(message = "Message is required")
        @Size(max = 2000, message = "Message must not exceed 2000 characters")
        String message
) {}

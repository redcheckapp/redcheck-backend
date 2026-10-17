package com.redcheck.backend.dto.update;

import com.redcheck.backend.entity.Feedback;
import jakarta.validation.constraints.Size;
import lombok.Builder;

// Partial update: a null field means "leave it as it is". To clear the note,
// send an empty string.
@Builder
public record AdminFeedbackUpdateDTO(
        Feedback.Status status,

        @Size(max = 2000, message = "Note must not exceed 2000 characters")
        String adminNote
) {}

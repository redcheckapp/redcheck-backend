package com.redcheck.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record RecurringTaskRequestDTO(
        @NotBlank(message = "Title is required")
        @Size(max = 100, message = "Title must not exceed 100 characters")
        String title,

        @Size(max = 500, message = "Description must not exceed 500 characters")
        String description,

        @NotBlank(message = "Frequency is required")
        @Pattern(
                regexp = "DAILY|WEEKLY|BIWEEKLY|MONTHLY|^[0-9*/ ,-]+$",
                message = "Invalid frequency format"
        )
        String frequency,

        @NotNull(message = "Subject is required")
        Long subjectId
) {}
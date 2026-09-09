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

        // Either one of the 4 simple presets, or a custom day-of-week cron
        // string built by the frontend (see recurrenceUtils.ts):
        // "0 0 0 * * <days>", <days> a comma-separated list of 0-6 (Sunday=0,
        // same as JS's Date#getDay()). Deliberately narrower than a generic
        // cron-charset pattern — restricting it to exactly the shape the UI
        // can build/display means every stored frequency can always be
        // parsed back into day checkboxes for editing (see
        // RecurringTasksModal.tsx's handleStartEdit).
        @NotBlank(message = "Frequency is required")
        @Pattern(
                regexp = "DAILY|WEEKLY|BIWEEKLY|MONTHLY|0 0 0 \\* \\* [0-6](,[0-6]){0,6}",
                message = "Invalid frequency format"
        )
        String frequency,

        @NotNull(message = "Subject is required")
        Long subjectId
) {}
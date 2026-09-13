package com.redcheck.backend.dto.request;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalTime;

@Builder
public record RecurringCalendarEventRequestDTO(
        @NotBlank(message = "Title is required")
        @Size(max = 100, message = "Title must not exceed 100 characters")
        String title,

        @Size(max = 500, message = "Description must not exceed 500 characters")
        String description,

        boolean allDay,

        // Ignored when allDay is true.
        LocalTime time,

        // Ignored when allDay is true. Zero is valid and means "punctual" —
        // e.g. a daily reminder with no real duration — mirroring how a
        // one-off CalendarEvent supports a punctual (zero-length) interval
        // via CalendarEventService#resolveEndDateTime. The frontend derives
        // this from (end - start) when the user leaves "End" blank, which
        // computes to exactly 0 in that case.
        @PositiveOrZero(message = "Duration must not be negative")
        Integer durationMinutes,

        // Same shape family as RecurringTaskRequestDTO.frequency, plus
        // YEARLY (needed for e.g. an annual birthday) — kept as its own
        // pattern/DTO rather than reusing Task's so the two recurrence
        // domains can diverge independently later.
        @NotBlank(message = "Frequency is required")
        @Pattern(
                regexp = "DAILY|WEEKLY|BIWEEKLY|MONTHLY|YEARLY|0 0 0 \\* \\* [0-6](,[0-6]){0,6}|MONTHLY:(LAST|[1-9]|[12][0-9]|3[01])",
                message = "Invalid frequency format"
        )
        String frequency,

        @FutureOrPresent(message = "End date must not be in the past")
        LocalDate endDate,

        Long categoryId
) {}

package com.redcheck.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record CalendarEventRequestDTO(
        @NotBlank(message = "Title is required")
        @Size(max = 100, message = "Title must not exceed 100 characters")
        String title,

        @Size(max = 500, message = "Description must not exceed 500 characters")
        String description,

        @NotNull(message = "Start date/time is required")
        LocalDateTime startDateTime,

        // Optional: null (and allDay=false) means a punctual event —
        // CalendarEventService stores it as equal to startDateTime, so
        // every persisted event is a well-formed interval. When allDay is
        // true, only the date part of this (or startDateTime, if omitted)
        // is used, letting an all-day event span multiple days (e.g. a
        // multi-day trip or exam week).
        LocalDateTime endDateTime,

        boolean allDay,

        // Optional: null means "no category" — an event doesn't need one.
        Long categoryId
) {}

package com.redcheck.backend.dto.response;

import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Builder
public record RecurringCalendarEventResponseDTO(
        Long id,
        String title,
        String description,
        boolean allDay,
        LocalTime time,
        Integer durationMinutes,
        String frequency,
        LocalDate endDate,
        boolean active,
        LocalDateTime createdDate,
        LocalDateTime latestGeneratedDate,
        // Server-computed preview of when this routine will next generate
        // an occurrence — see RecurringCalendarEventService#computeNextOccurrence.
        LocalDateTime nextOccurrence,
        Long categoryId
) {}

package com.redcheck.backend.dto.response;

import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Builder
public record RecurringTaskResponseDTO(
        Long id,
        String title,
        String description,
        String frequency,
        LocalTime time,
        LocalDate endDate,
        boolean active,
        LocalDateTime createdDate,
        LocalDateTime latestGeneratedDate,
        // Server-computed preview of when this routine will next fire —
        // null once it's inactive or past its endDate. See
        // RecurringTaskService#computeNextOccurrence. The frontend also has
        // its own client-side occurrence predictor (recurrenceUtils.ts) for
        // live-previewing an in-progress, not-yet-saved form — this field
        // is the authoritative one for an already-created routine.
        LocalDateTime nextOccurrence,
        // Streak/completion stats — see RecurringTaskService#computeStats.
        // currentStreak: consecutive completed occurrences counting back
        // from the most recent one (0 the moment the latest occurrence is
        // still pending, deliberately — no grace period for "not due yet",
        // see that method's own comment). longestStreak: the best run ever.
        // completionRate: totalCompleted / totalGenerated, 0 when nothing's
        // been generated yet (not null — a routine with no history yet
        // isn't "unknown", it's "0% so far").
        int currentStreak,
        int longestStreak,
        double completionRate,
        int totalGenerated,
        int totalCompleted,
        Long subjectId
) {}
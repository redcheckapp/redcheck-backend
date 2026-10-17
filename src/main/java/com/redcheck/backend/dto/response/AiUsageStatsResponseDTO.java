package com.redcheck.backend.dto.response;

import lombok.Builder;

import java.time.LocalDate;
import java.util.List;

// GET /admin/ai-usage. `daily` always has one entry per day of the window
// (zeros included), oldest first, so the frontend can chart it as-is.
@Builder
public record AiUsageStatsResponseDTO(
        int days,
        long todayPrioritizeCalls,
        long totalPrioritizeCalls,
        long totalOutcomeCalls,
        long failedCalls,
        long avgPrioritizeDurationMs,
        long distinctUsers,
        List<DailyUsage> daily
) {

    @Builder
    public record DailyUsage(
            LocalDate date,
            long prioritizeCalls,
            long outcomeCalls,
            long failedCalls
    ) {}
}

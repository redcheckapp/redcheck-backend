package com.redcheck.backend.dto.response;

import lombok.Builder;

import java.time.LocalDate;
import java.util.List;

// GET /admin/user-stats. Every count excludes the shared demo accounts and
// the admins themselves (see UserStatsService). Two independent notions of
// "active":
//  - seen*: had an authenticated request in the period (User.lastSeenAt).
//    Exact, but only from the day lastSeenAt started being recorded, and
//    with no history (just each user's latest value).
//  - acting*: did something real in the period (completed a task or created
//    one by hand). An approximation of "used it", but computed from existing
//    data, so it has full history — hence `daily` is activity-based.
@Builder
public record UserStatsResponseDTO(
        int days,
        long totalUsers,
        long googleUsers,
        long newUsers,
        long seenToday,
        long seen7d,
        long seen30d,
        long acting7d,
        long acting30d,
        List<DailyUsers> daily
) {

    @Builder
    public record DailyUsers(
            LocalDate date,
            long actingUsers,
            long signups
    ) {}
}

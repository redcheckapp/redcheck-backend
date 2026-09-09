package com.redcheck.backend.util;

import org.springframework.scheduling.support.CronExpression;

import java.time.LocalDateTime;
import java.util.Set;

public class FrequencyUtils {

    private static final Set<String> SIMPLE_VALUES = Set.of(
            "DAILY", "WEEKLY", "BIWEEKLY", "MONTHLY"
    );

    public static boolean isSimple(String frequency) {
        return SIMPLE_VALUES.contains(frequency);
    }

    // Custom (non-simple) frequencies are a restricted 6-field cron string
    // built by the frontend (see recurrenceUtils.ts): "0 0 0 * * <days>",
    // seconds/minutes/hours always pinned to midnight since
    // RecurringTaskSchedulerService only ever ticks once a day, only the
    // day-of-week field actually varies. CronExpression is Spring
    // Framework's own class (org.springframework.scheduling.support) — no
    // extra dependency needed, and RecurringTaskRequestDTO's @Pattern
    // already restricts `frequency` to exactly this shape or one of the 4
    // simple values, so parsing here should never fail on a value that
    // passed validation at creation/update time.
    public static LocalDateTime nextExecution(String frequency, LocalDateTime from) {
        if (isSimple(frequency)) {
            return switch (frequency) {
                case "DAILY" -> from.plusDays(1);
                case "WEEKLY" -> from.plusWeeks(1);
                case "BIWEEKLY" -> from.plusWeeks(2);
                case "MONTHLY" -> from.plusMonths(1);
                default -> throw new IllegalArgumentException("Unsupported frequency: " + frequency);
            };
        }

        LocalDateTime next = CronExpression.parse(frequency).next(from);
        if (next == null) {
            throw new IllegalArgumentException("Cron frequency has no future execution: " + frequency);
        }
        return next;
    }
}

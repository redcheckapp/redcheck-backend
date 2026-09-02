package com.redcheck.backend.util;

import java.time.LocalDateTime;
import java.util.Set;

public class FrequencyUtils {

    private static final Set<String> SIMPLE_VALUES = Set.of(
            "DAILY", "WEEKLY", "BIWEEKLY", "MONTHLY"
    );

    public static boolean isSimple(String frequency) {
        return SIMPLE_VALUES.contains(frequency);
    }

    public static LocalDateTime nextExecution(String frequency, LocalDateTime from) {
        return switch(frequency) {
            case "DAILY" -> from.plusDays(1);
            case "WEEKLY" -> from.plusWeeks(1);
            case "BIWEEKLY" -> from.plusWeeks(2);
            case "MONTHLY" -> from.plusMonths(1);
            default -> throw new IllegalArgumentException("Unsupported frequency: " + frequency);
        };
    }
}

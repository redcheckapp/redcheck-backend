package com.redcheck.backend.util;

import org.springframework.scheduling.support.CronExpression;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FrequencyUtils {

    private static final Set<String> SIMPLE_VALUES = Set.of(
            "DAILY", "WEEKLY", "BIWEEKLY", "MONTHLY"
    );

    // "Monthly on a specific day" custom frequency: "MONTHLY:<day>" where
    // <day> is 1-31 or the literal "LAST". Not expressed as cron — Spring's
    // CronExpression (unlike Quartz) has no "L" (last-day-of-month) special
    // character, and a plain numeric day-of-month field would just silently
    // skip short months (e.g. day 31 never firing in February) instead of
    // clamping to the month's actual last day, which reads as a bug from a
    // user's perspective ("why didn't my rent reminder show up this
    // month?"). So this is handled entirely in nextMonthlyDayExecution
    // below instead of being handed to CronExpression.
    private static final Pattern MONTHLY_DAY_PATTERN = Pattern.compile("^MONTHLY:(LAST|\\d{1,2})$");

    public static boolean isSimple(String frequency) {
        return SIMPLE_VALUES.contains(frequency);
    }

    // Custom (non-simple, non-monthly-day) frequencies are a restricted
    // 6-field cron string built by the frontend (see recurrenceUtils.ts):
    // "0 0 0 * * <days>", seconds/minutes/hours always pinned to midnight
    // since RecurringTaskSchedulerService only ever ticks once a day, only
    // the day-of-week field actually varies. CronExpression is Spring
    // Framework's own class (org.springframework.scheduling.support) — no
    // extra dependency needed, and RecurringTaskRequestDTO's @Pattern
    // already restricts `frequency` to exactly this shape, the 4 simple
    // values, or MONTHLY_DAY_PATTERN, so parsing here should never fail on
    // a value that passed validation at creation/update time.
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

        Matcher monthlyDayMatcher = MONTHLY_DAY_PATTERN.matcher(frequency);
        if (monthlyDayMatcher.matches()) {
            return nextMonthlyDayExecution(monthlyDayMatcher.group(1), from);
        }

        LocalDateTime next = CronExpression.parse(frequency).next(from);
        if (next == null) {
            throw new IllegalArgumentException("Cron frequency has no future execution: " + frequency);
        }
        return next;
    }

    private static LocalDateTime nextMonthlyDayExecution(String dayToken, LocalDateTime from) {
        LocalDate fromDate = from.toLocalDate();

        if ("LAST".equals(dayToken)) {
            LocalDate endOfThisMonth = YearMonth.from(fromDate).atEndOfMonth();
            LocalDate next = endOfThisMonth.isAfter(fromDate) ? endOfThisMonth : YearMonth.from(fromDate).plusMonths(1).atEndOfMonth();
            return next.atStartOfDay();
        }

        int day = Integer.parseInt(dayToken);
        LocalDate candidate = clampToMonth(YearMonth.from(fromDate), day);
        if (!candidate.isAfter(fromDate)) {
            candidate = clampToMonth(YearMonth.from(fromDate).plusMonths(1), day);
        }
        return candidate.atStartOfDay();
    }

    // Clamps a target day-of-month (e.g. 31) down to whatever the shortest
    // valid day actually is for that month (e.g. 28/29 in February) instead
    // of throwing or silently not matching.
    private static LocalDate clampToMonth(YearMonth yearMonth, int day) {
        return yearMonth.atDay(Math.min(day, yearMonth.lengthOfMonth()));
    }
}

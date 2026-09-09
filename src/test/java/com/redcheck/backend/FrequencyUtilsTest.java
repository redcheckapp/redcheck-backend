package com.redcheck.backend;

import com.redcheck.backend.util.FrequencyUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.DayOfWeek;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Unitary Tests - FrequencyUtils")
public class FrequencyUtilsTest {

    @Nested
    @DisplayName("Method: isSimple")
    class IsSimpleTests {

        @ParameterizedTest
        @ValueSource(strings = {"DAILY", "WEEKLY", "BIWEEKLY", "MONTHLY"})
        @DisplayName("Should return true for the 4 known presets")
        void isSimple_WhenPreset_ShouldReturnTrue(String preset) {
            assertTrue(FrequencyUtils.isSimple(preset));
        }

        @Test
        @DisplayName("Should return false for a custom day-of-week cron frequency")
        void isSimple_WhenCustomCron_ShouldReturnFalse() {
            assertFalse(FrequencyUtils.isSimple("0 0 0 * * 1,4"));
        }
    }

    @Nested
    @DisplayName("Method: nextExecution")
    class NextExecutionTests {

        @Test
        @DisplayName("DAILY should add one day")
        void nextExecution_Daily_ShouldAddOneDay() {
            LocalDateTime from = LocalDateTime.of(2026, 1, 1, 0, 0);
            assertEquals(from.plusDays(1), FrequencyUtils.nextExecution("DAILY", from));
        }

        @Test
        @DisplayName("WEEKLY should add one week")
        void nextExecution_Weekly_ShouldAddOneWeek() {
            LocalDateTime from = LocalDateTime.of(2026, 1, 1, 0, 0);
            assertEquals(from.plusWeeks(1), FrequencyUtils.nextExecution("WEEKLY", from));
        }

        @Test
        @DisplayName("BIWEEKLY should add two weeks")
        void nextExecution_Biweekly_ShouldAddTwoWeeks() {
            LocalDateTime from = LocalDateTime.of(2026, 1, 1, 0, 0);
            assertEquals(from.plusWeeks(2), FrequencyUtils.nextExecution("BIWEEKLY", from));
        }

        @Test
        @DisplayName("MONTHLY should add one month")
        void nextExecution_Monthly_ShouldAddOneMonth() {
            LocalDateTime from = LocalDateTime.of(2026, 1, 1, 0, 0);
            assertEquals(from.plusMonths(1), FrequencyUtils.nextExecution("MONTHLY", from));
        }

        @Test
        @DisplayName("Unknown simple-looking value should throw")
        void nextExecution_UnknownValue_ShouldThrow() {
            LocalDateTime from = LocalDateTime.of(2026, 1, 1, 0, 0);
            assertThrows(IllegalArgumentException.class, () -> FrequencyUtils.nextExecution("YEARLY", from));
        }

        @Test
        @DisplayName("Custom single-day cron should resolve to the next matching weekday")
        void nextExecution_CustomSingleDay_ShouldResolveToNextMatchingWeekday() {
            // 2026-01-01 is a Thursday; "every Monday" (cron day-of-week 1)
            // should resolve to the following Monday, 2026-01-05.
            LocalDateTime from = LocalDateTime.of(2026, 1, 1, 0, 0);
            assertEquals(DayOfWeek.THURSDAY, from.getDayOfWeek());

            LocalDateTime next = FrequencyUtils.nextExecution("0 0 0 * * 1", from);

            assertEquals(LocalDateTime.of(2026, 1, 5, 0, 0), next);
            assertEquals(DayOfWeek.MONDAY, next.getDayOfWeek());
        }

        @Test
        @DisplayName("Custom multi-day cron should resolve to the nearest of the selected weekdays")
        void nextExecution_CustomMultiDay_ShouldResolveToNearestSelectedWeekday() {
            // 2026-01-01 is a Thursday; "Monday and Friday" (1,5) should
            // resolve to the very next day, Friday 2026-01-02.
            LocalDateTime from = LocalDateTime.of(2026, 1, 1, 0, 0);

            LocalDateTime next = FrequencyUtils.nextExecution("0 0 0 * * 1,5", from);

            assertEquals(LocalDateTime.of(2026, 1, 2, 0, 0), next);
            assertEquals(DayOfWeek.FRIDAY, next.getDayOfWeek());
        }

        @Test
        @DisplayName("Malformed custom cron should throw")
        void nextExecution_MalformedCron_ShouldThrow() {
            LocalDateTime from = LocalDateTime.of(2026, 1, 1, 0, 0);
            assertThrows(IllegalArgumentException.class, () -> FrequencyUtils.nextExecution("not a cron", from));
        }

        @Test
        @DisplayName("Monthly-on-day-15 should resolve to the 15th of next month, from the 1st")
        void nextExecution_MonthlyDay15_ShouldResolveToThe15thOfSameMonth() {
            LocalDateTime from = LocalDateTime.of(2026, 1, 1, 0, 0);
            assertEquals(LocalDateTime.of(2026, 1, 15, 0, 0), FrequencyUtils.nextExecution("MONTHLY:15", from));
        }

        @Test
        @DisplayName("Monthly-on-day-15 should roll over to next month once already past the 15th")
        void nextExecution_MonthlyDay15_ShouldRollOverPastThe15th() {
            LocalDateTime from = LocalDateTime.of(2026, 1, 20, 0, 0);
            assertEquals(LocalDateTime.of(2026, 2, 15, 0, 0), FrequencyUtils.nextExecution("MONTHLY:15", from));
        }

        @Test
        @DisplayName("Monthly-on-day-31 should clamp to February's actual last day instead of skipping it")
        void nextExecution_MonthlyDay31_ShouldClampInShortMonth() {
            LocalDateTime from = LocalDateTime.of(2026, 1, 31, 0, 0);
            // 2026 is not a leap year — February has 28 days.
            assertEquals(LocalDateTime.of(2026, 2, 28, 0, 0), FrequencyUtils.nextExecution("MONTHLY:31", from));
        }

        @Test
        @DisplayName("Monthly-on-LAST should resolve to this month's last day when still ahead")
        void nextExecution_MonthlyLast_ShouldResolveToThisMonthsLastDay() {
            LocalDateTime from = LocalDateTime.of(2026, 2, 10, 0, 0);
            assertEquals(LocalDateTime.of(2026, 2, 28, 0, 0), FrequencyUtils.nextExecution("MONTHLY:LAST", from));
        }

        @Test
        @DisplayName("Monthly-on-LAST should roll over to next month's last day once already there")
        void nextExecution_MonthlyLast_ShouldRollOverOnceAlreadyLastDay() {
            LocalDateTime from = LocalDateTime.of(2026, 2, 28, 0, 0);
            assertEquals(LocalDateTime.of(2026, 3, 31, 0, 0), FrequencyUtils.nextExecution("MONTHLY:LAST", from));
        }
    }
}

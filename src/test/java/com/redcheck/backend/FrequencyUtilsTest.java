package com.redcheck.backend;

import com.redcheck.backend.util.FrequencyUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Unitary Tests - FrequencyUtils")
public class FrequencyUtilsTest {

    @Nested
    @DisplayName("Method: isSimple")
    class IsSimpleTests {

        @ParameterizedTest
        @ValueSource(strings = {"DAILY", "WEEKLY", "BIWEEKLY", "MONTHLY", "YEARLY"})
        @DisplayName("Should return true for the 5 known presets")
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
        @DisplayName("YEARLY should add one year")
        void nextExecution_Yearly_ShouldAddOneYear() {
            LocalDateTime from = LocalDateTime.of(2026, 1, 1, 0, 0);
            assertEquals(from.plusYears(1), FrequencyUtils.nextExecution("YEARLY", from));
        }

        @Test
        @DisplayName("Unknown simple-looking value should throw")
        void nextExecution_UnknownValue_ShouldThrow() {
            LocalDateTime from = LocalDateTime.of(2026, 1, 1, 0, 0);
            assertThrows(IllegalArgumentException.class, () -> FrequencyUtils.nextExecution("ANNUALLY", from));
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

    @Nested
    @DisplayName("Method: firstOccurrenceOnOrAfter")
    class FirstOccurrenceOnOrAfterTests {

        // 2026-10-20 is a Tuesday.
        private final LocalDate tuesday = LocalDate.of(2026, 10, 20);

        @Test
        @DisplayName("A simple preset should start on the given date itself")
        void firstOccurrence_ForSimplePreset_ShouldReturnSameDate() {
            assertEquals(tuesday, FrequencyUtils.firstOccurrenceOnOrAfter("WEEKLY", tuesday));
        }

        @Test
        @DisplayName("A custom weekday pattern should return the date when it matches")
        void firstOccurrence_ForMatchingWeekday_ShouldReturnSameDate() {
            assertEquals(tuesday, FrequencyUtils.firstOccurrenceOnOrAfter("0 0 0 * * 2,5", tuesday));
        }

        @Test
        @DisplayName("A custom Mon/Thu pattern starting on a Tuesday should land on Thursday, not Wednesday")
        void firstOccurrence_ForNonMatchingWeekday_ShouldSkipToNextMatch() {
            assertEquals(LocalDate.of(2026, 10, 22), FrequencyUtils.firstOccurrenceOnOrAfter("0 0 0 * * 1,4", tuesday));
        }

        @Test
        @DisplayName("A custom day-of-month pattern should land on that day")
        void firstOccurrence_ForMonthlyDay_ShouldReturnThatDay() {
            assertEquals(LocalDate.of(2026, 11, 15), FrequencyUtils.firstOccurrenceOnOrAfter("MONTHLY:15", tuesday));
            assertEquals(tuesday, FrequencyUtils.firstOccurrenceOnOrAfter("MONTHLY:20", tuesday));
            assertEquals(LocalDate.of(2026, 10, 31), FrequencyUtils.firstOccurrenceOnOrAfter("MONTHLY:LAST", tuesday));
        }
    }

    @Nested
    @DisplayName("Methods: nextOccurrenceOnOrAfter / isDueOn")
    class NextOccurrenceOnOrAfterTests {

        // 2026-10-20 is a Tuesday.
        private final LocalDate tuesday = LocalDate.of(2026, 10, 20);

        @Test
        @DisplayName("Never generated: falls back to the first occurrence the pattern selects")
        void nextOccurrence_WhenNeverGenerated_ShouldUseFirstOccurrence() {
            assertEquals(LocalDate.of(2026, 10, 22), FrequencyUtils.nextOccurrenceOnOrAfter("0 0 0 * * 1,4", null, tuesday));
            assertFalse(FrequencyUtils.isDueOn("0 0 0 * * 1,4", null, tuesday));
            assertTrue(FrequencyUtils.isDueOn("DAILY", null, tuesday));
        }

        @Test
        @DisplayName("After a gap, a custom weekday pattern catches up to its next selected day, never firing in between")
        void nextOccurrence_AfterGap_ShouldSkipToNextSelectedDay() {
            // Last generated on a Monday months ago (paused and reactivated).
            LocalDateTime staleMonday = LocalDate.of(2026, 6, 1).atStartOfDay().plusSeconds(1);
            assertEquals(LocalDate.of(2026, 10, 22), FrequencyUtils.nextOccurrenceOnOrAfter("0 0 0 * * 1,4", staleMonday, tuesday));
            assertFalse(FrequencyUtils.isDueOn("0 0 0 * * 1,4", staleMonday, tuesday));
            assertTrue(FrequencyUtils.isDueOn("0 0 0 * * 1,4", staleMonday, LocalDate.of(2026, 10, 22)));
        }

        @Test
        @DisplayName("Already generated today: the next occurrence is a later day, so it's not due again")
        void isDueOn_WhenAlreadyGeneratedToday_ShouldBeFalse() {
            assertFalse(FrequencyUtils.isDueOn("DAILY", tuesday.atStartOfDay().plusSeconds(1), tuesday));
            assertFalse(FrequencyUtils.isDueOn("0 0 0 * * 2", tuesday.atStartOfDay().plusSeconds(1), tuesday));
        }

        @Test
        @DisplayName("A monthly day-31 pattern catches up across short months by clamping")
        void nextOccurrence_ForMonthlyDay31_ShouldClamp() {
            LocalDateTime jan31 = LocalDate.of(2026, 1, 31).atStartOfDay().plusSeconds(1);
            assertEquals(LocalDate.of(2026, 2, 28), FrequencyUtils.nextOccurrenceOnOrAfter("MONTHLY:31", jan31, LocalDate.of(2026, 2, 1)));
            assertEquals(LocalDate.of(2026, 3, 31), FrequencyUtils.nextOccurrenceOnOrAfter("MONTHLY:31", jan31, LocalDate.of(2026, 3, 1)));
        }
    }
}

package com.redcheck.backend;

import com.redcheck.backend.entity.CalendarEvent;
import com.redcheck.backend.entity.RecurringCalendarEvent;
import com.redcheck.backend.repository.CalendarEventRepository;
import com.redcheck.backend.repository.RecurringCalendarEventRepository;
import com.redcheck.backend.service.RecurringCalendarEventSchedulerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unitary Tests - RecurringCalendarEventSchedulerService")
public class RecurringCalendarEventSchedulerServiceTest {

    @Mock
    private RecurringCalendarEventRepository recurringCalendarEventRepository;

    @Mock
    private CalendarEventRepository calendarEventRepository;

    @InjectMocks
    private RecurringCalendarEventSchedulerService recurringCalendarEventSchedulerService;

    // ISO Monday=1..Sunday=7 -> cron Sunday=0..Saturday=6
    private static int cronDay(LocalDate date) {
        return date.getDayOfWeek().getValue() % 7;
    }

    private static RecurringCalendarEvent template(String title, String frequency) {
        return RecurringCalendarEvent.builder().title(title).frequency(frequency).allDay(true).active(true).build();
    }

    @Nested
    @DisplayName("Method: generateEvents")
    class GenerateEventsTests {

        @Test
        @DisplayName("Never generated: a custom weekday pattern should only create its first event on a selected day")
        void generateEvents_CustomWeekday_FirstOccurrence_ShouldRespectPattern() {
            // GIVEN
            RecurringCalendarEvent dueToday = template("Due today", "0 0 0 * * " + cronDay(LocalDate.now()));
            RecurringCalendarEvent dueTomorrow = template("Due tomorrow", "0 0 0 * * " + cronDay(LocalDate.now().plusDays(1)));
            when(recurringCalendarEventRepository.findAllByActiveTrue()).thenReturn(List.of(dueToday, dueTomorrow));

            // WHEN
            recurringCalendarEventSchedulerService.generateEvents();

            // THEN
            verify(calendarEventRepository, times(1)).save(any(CalendarEvent.class));
            verify(recurringCalendarEventRepository, times(1)).save(dueToday);
            verify(recurringCalendarEventRepository, never()).save(dueTomorrow);
        }

        @Test
        @DisplayName("After a gap: a custom weekday pattern should not fire on a day it doesn't select")
        void generateEvents_CustomWeekday_AfterGap_ShouldNotGenerateOnUnselectedDay() {
            // GIVEN: selects only tomorrow's weekday, last generated a year ago.
            RecurringCalendarEvent stale = template("Stale", "0 0 0 * * " + cronDay(LocalDate.now().plusDays(1)));
            stale.setLatestGeneratedDate(LocalDate.now().minusYears(1).atStartOfDay());
            when(recurringCalendarEventRepository.findAllByActiveTrue()).thenReturn(List.of(stale));

            // WHEN
            recurringCalendarEventSchedulerService.generateEvents();

            // THEN
            verify(calendarEventRepository, never()).save(any(CalendarEvent.class));
            verify(recurringCalendarEventRepository, never()).save(any(RecurringCalendarEvent.class));
        }

        @Test
        @DisplayName("A simple preset should still start on the first tick")
        void generateEvents_SimplePreset_FirstOccurrence_ShouldGenerate() {
            // GIVEN
            RecurringCalendarEvent yearly = template("Birthday", "YEARLY");
            when(recurringCalendarEventRepository.findAllByActiveTrue()).thenReturn(List.of(yearly));

            // WHEN
            recurringCalendarEventSchedulerService.generateEvents();

            // THEN
            verify(calendarEventRepository, times(1)).save(any(CalendarEvent.class));
        }
    }
}

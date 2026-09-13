package com.redcheck.backend.service;

import com.redcheck.backend.entity.CalendarEvent;
import com.redcheck.backend.entity.RecurringCalendarEvent;
import com.redcheck.backend.repository.CalendarEventRepository;
import com.redcheck.backend.repository.RecurringCalendarEventRepository;
import com.redcheck.backend.util.FrequencyUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

// Mirrors RecurringTaskSchedulerService's daily-materialization approach:
// a routine (RecurringCalendarEvent) is a template, and this scheduler
// creates real CalendarEvent rows from it. Unlike Task, a CalendarEvent has
// no "completed" concept, so there's no equivalent of that scheduler's
// "don't generate a duplicate while the last occurrence is still pending"
// fallback — occurrences are timing-only here, and this runs once a day.
@Slf4j
@Service
@RequiredArgsConstructor
public class RecurringCalendarEventSchedulerService {

    private final RecurringCalendarEventRepository recurringCalendarEventRepository;
    private final CalendarEventRepository calendarEventRepository;

    @Transactional
    @Scheduled(cron = "0 0 0 * * *")
    public void generateEvents() {
        log.info("Starting generation of recurring calendar events...");
        List<RecurringCalendarEvent> activeTemplates = recurringCalendarEventRepository.findAllByActiveTrue();

        for (RecurringCalendarEvent template : activeTemplates) {
            // One malformed/unparseable frequency must not abort generation
            // for every other template in this run.
            try {
                if (isAfterEndDate(template)) {
                    template.setActive(false);
                    recurringCalendarEventRepository.save(template);
                    continue;
                }

                if (shouldGenerate(template)) {
                    calendarEventRepository.save(buildOccurrence(template));
                    template.setLatestGeneratedDate(LocalDateTime.now());

                    if (hasReachedEndDate(template)) {
                        template.setActive(false);
                    }

                    recurringCalendarEventRepository.save(template);
                }
            } catch (Exception e) {
                log.error("Failed to process recurring calendar event {} (frequency='{}'): {}",
                        template.getId(), template.getFrequency(), e.getMessage(), e);
            }
        }
        log.info("Recurring calendar events generation completed.");
    }

    private boolean isAfterEndDate(RecurringCalendarEvent template) {
        return template.getEndDate() != null && LocalDate.now().isAfter(template.getEndDate());
    }

    private boolean hasReachedEndDate(RecurringCalendarEvent template) {
        return template.getEndDate() != null && !LocalDate.now().isBefore(template.getEndDate());
    }

    private boolean shouldGenerate(RecurringCalendarEvent template) {
        if (template.getLatestGeneratedDate() == null) {
            return true;
        }

        LocalDateTime next = FrequencyUtils.nextExecution(template.getFrequency(), template.getLatestGeneratedDate());
        return !LocalDateTime.now().isBefore(next);
    }

    private CalendarEvent buildOccurrence(RecurringCalendarEvent template) {
        LocalDate today = LocalDate.now();

        if (template.isAllDay()) {
            // 23:59:59, not LocalTime.MAX — see CalendarEventService
            // #resolveEndDateTime's comment on the MySQL rounding-up bug
            // a persisted sub-second value causes.
            return CalendarEvent.builder()
                    .title(template.getTitle())
                    .description(template.getDescription())
                    .startDateTime(today.atStartOfDay())
                    .endDateTime(today.atTime(23, 59, 59))
                    .allDay(true)
                    .category(template.getCategory())
                    .recurringEvent(template)
                    .user(template.getUser())
                    .build();
        }

        LocalTime time = template.getTime() != null ? template.getTime() : LocalTime.MIDNIGHT;
        LocalDateTime start = today.atTime(time);
        int durationMinutes = template.getDurationMinutes() != null ? template.getDurationMinutes() : 0;

        return CalendarEvent.builder()
                .title(template.getTitle())
                .description(template.getDescription())
                .startDateTime(start)
                .endDateTime(start.plusMinutes(durationMinutes))
                .allDay(false)
                .category(template.getCategory())
                .recurringEvent(template)
                .user(template.getUser())
                .build();
    }
}

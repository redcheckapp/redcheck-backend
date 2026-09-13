package com.redcheck.backend.service;

import com.redcheck.backend.dto.request.CalendarEventRequestDTO;
import com.redcheck.backend.dto.response.CalendarEventResponseDTO;
import com.redcheck.backend.entity.CalendarEvent;
import com.redcheck.backend.entity.EventCategory;
import com.redcheck.backend.entity.User;
import com.redcheck.backend.exception.CalendarEventNotFoundException;
import com.redcheck.backend.exception.CalendarEventNotOwnedException;
import com.redcheck.backend.exception.EventCategoryNotFoundException;
import com.redcheck.backend.exception.EventCategoryNotOwnedException;
import com.redcheck.backend.repository.CalendarEventRepository;
import com.redcheck.backend.repository.EventCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CalendarEventService {

    private final CalendarEventRepository calendarEventRepository;
    private final EventCategoryRepository eventCategoryRepository;

    // Every event (pending or past) whose interval overlaps [from, to] —
    // same "one call per visible calendar view" contract as
    // TaskService#getTasksForDateRange.
    public List<CalendarEventResponseDTO> getEventsForDateRange(User currentUser, LocalDate from, LocalDate to) {
        LocalDateTime startOfDay = from.atStartOfDay();
        LocalDateTime endOfDay = to.atTime(LocalTime.MAX);

        return calendarEventRepository.findAllByUser_IdAndDateRange(currentUser.getId(), startOfDay, endOfDay)
                .stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public CalendarEventResponseDTO createEvent(CalendarEventRequestDTO requestDTO, User currentUser) {
        EventCategory category = resolveCategory(requestDTO.categoryId(), currentUser);

        LocalDateTime start = requestDTO.allDay()
                ? requestDTO.startDateTime().toLocalDate().atStartOfDay()
                : requestDTO.startDateTime();
        LocalDateTime end = resolveEndDateTime(start, requestDTO.endDateTime(), requestDTO.allDay());

        CalendarEvent event = CalendarEvent.builder()
                .title(requestDTO.title())
                .description(requestDTO.description())
                .startDateTime(start)
                .endDateTime(end)
                .allDay(requestDTO.allDay())
                .category(category)
                .user(currentUser)
                .build();

        calendarEventRepository.save(event);
        return toResponseDTO(event);
    }

    @Transactional
    public CalendarEventResponseDTO updateEvent(Long eventId, CalendarEventRequestDTO requestDTO, User currentUser) {
        CalendarEvent event = getOwnedEvent(eventId, currentUser);
        EventCategory category = resolveCategory(requestDTO.categoryId(), currentUser);

        LocalDateTime start = requestDTO.allDay()
                ? requestDTO.startDateTime().toLocalDate().atStartOfDay()
                : requestDTO.startDateTime();
        LocalDateTime end = resolveEndDateTime(start, requestDTO.endDateTime(), requestDTO.allDay());

        event.setTitle(requestDTO.title());
        event.setDescription(requestDTO.description());
        event.setStartDateTime(start);
        event.setEndDateTime(end);
        event.setAllDay(requestDTO.allDay());
        event.setCategory(category);

        calendarEventRepository.save(event);
        return toResponseDTO(event);
    }

    @Transactional
    public void deleteEvent(Long eventId, User currentUser) {
        CalendarEvent event = getOwnedEvent(eventId, currentUser);
        calendarEventRepository.delete(event);
    }

    // --- Auxiliary methods ---

    private EventCategory resolveCategory(Long categoryId, User currentUser) {
        if (categoryId == null) {
            return null;
        }

        EventCategory category = eventCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new EventCategoryNotFoundException(categoryId));

        if (!category.getUser().getId().equals(currentUser.getId())) {
            throw new EventCategoryNotOwnedException();
        }

        return category;
    }

    // A punctual event (allDay=false, no explicit end) is stored with
    // endDateTime equal to startDateTime, and an all-day event's end is
    // normalized to the end of its end day — keeps every CalendarEvent row
    // a well-formed, non-null interval (see CalendarEvent#endDateTime), so
    // range queries never need separate null-handling logic.
    //
    // Deliberately LocalTime.of(23, 59, 59) here, NOT LocalTime.MAX
    // (23:59:59.999999999): this value gets persisted (unlike the
    // query-bound use of LocalTime.MAX in getEventsForDateRange above),
    // and MySQL's JDBC driver rounds a DATETIME column's unsupported
    // sub-second fraction up rather than truncating it — .999999999
    // silently became the *next* day at 00:00:00 on write, making an
    // all-day event spill onto the following day. A whole-second value
    // has nothing to round.
    private LocalDateTime resolveEndDateTime(LocalDateTime start, LocalDateTime end, boolean allDay) {
        if (allDay) {
            LocalDate endDate = end != null ? end.toLocalDate() : start.toLocalDate();
            return endDate.atTime(23, 59, 59);
        }

        if (end == null) {
            return start;
        }

        if (end.isBefore(start)) {
            throw new IllegalArgumentException("endDateTime must not be before startDateTime");
        }

        return end;
    }

    private CalendarEvent getOwnedEvent(Long eventId, User currentUser) {
        CalendarEvent event = calendarEventRepository.findById(eventId)
                .orElseThrow(() -> new CalendarEventNotFoundException(eventId));

        if (!event.getUser().getId().equals(currentUser.getId())) {
            throw new CalendarEventNotOwnedException();
        }

        return event;
    }

    private CalendarEventResponseDTO toResponseDTO(CalendarEvent event) {
        return CalendarEventResponseDTO.builder()
                .id(event.getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .startDateTime(event.getStartDateTime())
                .endDateTime(event.getEndDateTime())
                .allDay(event.isAllDay())
                .categoryId(event.getCategory() != null ? event.getCategory().getId() : null)
                .recurringEventId(event.getRecurringEvent() != null ? event.getRecurringEvent().getId() : null)
                .build();
    }
}

package com.redcheck.backend.service;

import com.redcheck.backend.dto.request.RecurringCalendarEventRequestDTO;
import com.redcheck.backend.dto.response.RecurringCalendarEventResponseDTO;
import com.redcheck.backend.entity.EventCategory;
import com.redcheck.backend.entity.RecurringCalendarEvent;
import com.redcheck.backend.entity.User;
import com.redcheck.backend.exception.EventCategoryNotFoundException;
import com.redcheck.backend.exception.EventCategoryNotOwnedException;
import com.redcheck.backend.exception.RecurringCalendarEventNotFoundException;
import com.redcheck.backend.exception.RecurringCalendarEventNotOwnedException;
import com.redcheck.backend.repository.CalendarEventRepository;
import com.redcheck.backend.repository.EventCategoryRepository;
import com.redcheck.backend.repository.RecurringCalendarEventRepository;
import com.redcheck.backend.util.FrequencyUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecurringCalendarEventService {

    private final RecurringCalendarEventRepository recurringCalendarEventRepository;
    private final CalendarEventRepository calendarEventRepository;
    private final EventCategoryRepository eventCategoryRepository;

    public List<RecurringCalendarEventResponseDTO> getAllRecurringEvents(User currentUser, Boolean active) {
        List<RecurringCalendarEvent> raw = active != null
                ? recurringCalendarEventRepository.findAllByUser_IdAndActive(currentUser.getId(), active)
                : recurringCalendarEventRepository.findAllByUser_Id(currentUser.getId());

        return raw.stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public RecurringCalendarEventResponseDTO createRecurringEvent(RecurringCalendarEventRequestDTO requestDTO, User currentUser) {
        EventCategory category = resolveCategory(requestDTO.categoryId(), currentUser);

        RecurringCalendarEvent recurringEvent = RecurringCalendarEvent.builder()
                .title(requestDTO.title())
                .description(requestDTO.description())
                .allDay(requestDTO.allDay())
                .time(requestDTO.allDay() ? null : requestDTO.time())
                .durationMinutes(requestDTO.allDay() ? null : requestDTO.durationMinutes())
                .frequency(requestDTO.frequency())
                .endDate(requestDTO.endDate())
                .category(category)
                .active(true)
                .user(currentUser)
                .build();

        recurringCalendarEventRepository.save(recurringEvent);
        return toResponseDTO(recurringEvent);
    }

    @Transactional
    public RecurringCalendarEventResponseDTO updateRecurringEvent(Long recurringEventId, RecurringCalendarEventRequestDTO requestDTO, User currentUser) {
        RecurringCalendarEvent recurringEvent = getOwnedRecurringEvent(recurringEventId, currentUser);
        EventCategory category = resolveCategory(requestDTO.categoryId(), currentUser);

        recurringEvent.setTitle(requestDTO.title());
        recurringEvent.setDescription(requestDTO.description());
        recurringEvent.setAllDay(requestDTO.allDay());
        recurringEvent.setTime(requestDTO.allDay() ? null : requestDTO.time());
        recurringEvent.setDurationMinutes(requestDTO.allDay() ? null : requestDTO.durationMinutes());
        recurringEvent.setFrequency(requestDTO.frequency());
        recurringEvent.setEndDate(requestDTO.endDate());
        recurringEvent.setCategory(category);

        recurringCalendarEventRepository.save(recurringEvent);
        return toResponseDTO(recurringEvent);
    }

    @Transactional
    public void deleteRecurringEvent(Long recurringEventId, User currentUser) {
        RecurringCalendarEvent recurringEvent = getOwnedRecurringEvent(recurringEventId, currentUser);

        calendarEventRepository.detachFromRecurringEvent(recurringEvent);

        recurringCalendarEventRepository.delete(recurringEvent);
    }

    @Transactional
    public RecurringCalendarEventResponseDTO setActive(Long recurringEventId, boolean active, User currentUser) {
        RecurringCalendarEvent recurringEvent = getOwnedRecurringEvent(recurringEventId, currentUser);

        recurringEvent.setActive(active);

        recurringCalendarEventRepository.save(recurringEvent);
        return toResponseDTO(recurringEvent);
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

    private RecurringCalendarEvent getOwnedRecurringEvent(Long recurringEventId, User currentUser) {
        RecurringCalendarEvent recurringEvent = recurringCalendarEventRepository.findById(recurringEventId)
                .orElseThrow(() -> new RecurringCalendarEventNotFoundException(recurringEventId));

        if (!recurringEvent.getUser().getId().equals(currentUser.getId())) {
            throw new RecurringCalendarEventNotOwnedException();
        }

        return recurringEvent;
    }

    private RecurringCalendarEventResponseDTO toResponseDTO(RecurringCalendarEvent recurringEvent) {
        return RecurringCalendarEventResponseDTO.builder()
                .id(recurringEvent.getId())
                .title(recurringEvent.getTitle())
                .description(recurringEvent.getDescription())
                .allDay(recurringEvent.isAllDay())
                .time(recurringEvent.getTime())
                .durationMinutes(recurringEvent.getDurationMinutes())
                .frequency(recurringEvent.getFrequency())
                .endDate(recurringEvent.getEndDate())
                .active(recurringEvent.isActive())
                .createdDate(recurringEvent.getCreatedDate())
                .latestGeneratedDate(recurringEvent.getLatestGeneratedDate())
                .nextOccurrence(computeNextOccurrence(recurringEvent))
                .categoryId(recurringEvent.getCategory() != null ? recurringEvent.getCategory().getId() : null)
                .build();
    }

    // Read-only preview, never mutates state — same contract as
    // RecurringTaskService#computeNextOccurrence.
    private LocalDateTime computeNextOccurrence(RecurringCalendarEvent recurringEvent) {
        if (!recurringEvent.isActive()) {
            return null;
        }

        LocalDate nextDate;
        if (recurringEvent.getLatestGeneratedDate() == null) {
            nextDate = LocalDate.now().plusDays(1);
        } else {
            nextDate = FrequencyUtils.nextExecution(recurringEvent.getFrequency(), recurringEvent.getLatestGeneratedDate()).toLocalDate();
        }

        if (recurringEvent.getEndDate() != null && nextDate.isAfter(recurringEvent.getEndDate())) {
            return null;
        }

        return recurringEvent.isAllDay() || recurringEvent.getTime() == null
                ? nextDate.atStartOfDay()
                : nextDate.atTime(recurringEvent.getTime());
    }
}

package com.redcheck.backend.service;

import com.redcheck.backend.dto.request.EventCategoryRequestDTO;
import com.redcheck.backend.dto.response.EventCategoryResponseDTO;
import com.redcheck.backend.entity.EventCategory;
import com.redcheck.backend.entity.User;
import com.redcheck.backend.exception.EventCategoryNotFoundException;
import com.redcheck.backend.exception.EventCategoryNotOwnedException;
import com.redcheck.backend.repository.CalendarEventRepository;
import com.redcheck.backend.repository.EventCategoryRepository;
import com.redcheck.backend.repository.RecurringCalendarEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventCategoryService {

    private final EventCategoryRepository eventCategoryRepository;
    private final CalendarEventRepository calendarEventRepository;
    private final RecurringCalendarEventRepository recurringCalendarEventRepository;

    public List<EventCategoryResponseDTO> getAllCategories(User currentUser) {
        return eventCategoryRepository.findAllByUser_Id(currentUser.getId())
                .stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public EventCategoryResponseDTO createCategory(EventCategoryRequestDTO requestDTO, User currentUser) {
        EventCategory category = EventCategory.builder()
                .name(requestDTO.name())
                .color(requestDTO.color())
                .user(currentUser)
                .build();

        eventCategoryRepository.save(category);
        return toResponseDTO(category);
    }

    @Transactional
    public EventCategoryResponseDTO updateCategory(Long categoryId, EventCategoryRequestDTO requestDTO, User currentUser) {
        EventCategory category = getOwnedCategory(categoryId, currentUser);

        category.setName(requestDTO.name());
        category.setColor(requestDTO.color());

        eventCategoryRepository.save(category);
        return toResponseDTO(category);
    }

    // Deleting a category never deletes the events/routines that use it —
    // it just detaches them (they fall back to "no category"), same
    // "unlink rather than cascade-delete" reasoning as
    // RecurringTaskService#deleteRecurringTask detaching its generated
    // Tasks instead of removing them.
    @Transactional
    public void deleteCategory(Long categoryId, User currentUser) {
        EventCategory category = getOwnedCategory(categoryId, currentUser);

        calendarEventRepository.detachFromCategory(category);
        recurringCalendarEventRepository.detachFromCategory(category);

        eventCategoryRepository.delete(category);
    }

    private EventCategory getOwnedCategory(Long categoryId, User currentUser) {
        EventCategory category = eventCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new EventCategoryNotFoundException(categoryId));

        if (!category.getUser().getId().equals(currentUser.getId())) {
            throw new EventCategoryNotOwnedException();
        }

        return category;
    }

    private EventCategoryResponseDTO toResponseDTO(EventCategory category) {
        return EventCategoryResponseDTO.builder()
                .id(category.getId())
                .name(category.getName())
                .color(category.getColor())
                .build();
    }
}

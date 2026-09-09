package com.redcheck.backend.service;

import com.redcheck.backend.dto.request.RecurringTaskRequestDTO;
import com.redcheck.backend.dto.response.RecurringTaskResponseDTO;
import com.redcheck.backend.dto.update.RecurringTaskActiveDTO;
import com.redcheck.backend.entity.RecurringTask;
import com.redcheck.backend.entity.Subject;
import com.redcheck.backend.entity.User;
import com.redcheck.backend.exception.*;
import com.redcheck.backend.repository.RecurringTaskRepository;
import com.redcheck.backend.repository.SubjectRepository;
import com.redcheck.backend.repository.TaskRepository;
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
public class RecurringTaskService {

    private final RecurringTaskRepository recurringTaskRepository;
    private final SubjectRepository subjectRepository;
    private final TaskRepository taskRepository;

    public List<RecurringTaskResponseDTO> getAllRecurringTask(User currentUser, Long subjectId, Boolean active) {

        List<RecurringTask> rawRecurringTask;

        if (active != null) {
            rawRecurringTask = recurringTaskRepository.findAllBySubject_User_IdAndActive(currentUser.getId(), active);
        } else {
            rawRecurringTask = recurringTaskRepository.findAllBySubject_User_Id(currentUser.getId());
        }

        // At this point, only the subjectId filter is applied in memory
        return rawRecurringTask.stream()
                .map(this::toResponseDTO)
                .filter(recurringTask -> subjectId == null || recurringTask.subjectId().equals(subjectId))
                .collect(Collectors.toList());
    }

    @Transactional
    public RecurringTaskResponseDTO createRecurringTask(Long subjectId, RecurringTaskRequestDTO requestDTO, User currentUser) {

        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new SubjectNotFoundException(subjectId));

        if (!subject.getUser().getId().equals(currentUser.getId())) {
            throw new SubjectNotOwnedException();
        }

        RecurringTask recurringTask = RecurringTask.builder()
                .title(requestDTO.title())
                .description(requestDTO.description())
                .frequency(requestDTO.frequency())
                .time(requestDTO.time())
                .endDate(requestDTO.endDate())
                .active(true)
                .subject(subject)
                .build();

        recurringTaskRepository.save(recurringTask);
        return toResponseDTO(recurringTask);
    }

    @Transactional
    public RecurringTaskResponseDTO updateRecurringTask(Long subjectId, Long recurringTaskId, RecurringTaskRequestDTO requestDTO, User currentUser) {

        RecurringTask recurringTask = getOwnedRecurringTask(subjectId, recurringTaskId, currentUser);

        Subject newSubject = subjectRepository.findById(requestDTO.subjectId())
                .orElseThrow(() -> new SubjectNotFoundException(requestDTO.subjectId()));

        if (!newSubject.getUser().getId().equals(currentUser.getId())) {
            throw new SubjectNotOwnedException();
        }

        recurringTask.setTitle(requestDTO.title());
        recurringTask.setDescription(requestDTO.description());
        recurringTask.setFrequency(requestDTO.frequency());
        recurringTask.setTime(requestDTO.time());
        recurringTask.setEndDate(requestDTO.endDate());
        recurringTask.setSubject(newSubject);

        recurringTaskRepository.save(recurringTask);
        return toResponseDTO(recurringTask);
    }

    @Transactional
    public void deleteRecurringTask(Long subjectId, Long recurringTaskId, User currentUser) {

        RecurringTask recurringTask = getOwnedRecurringTask(subjectId, recurringTaskId, currentUser);

        taskRepository.detachFromRecurringTask(recurringTask);

        recurringTaskRepository.delete(recurringTask);
    }

    @Transactional
    public RecurringTaskResponseDTO activateRecurringTask(Long subjectId, Long recurringTaskId, RecurringTaskActiveDTO requestDTO, User currentUser) {

        RecurringTask recurringTask = getOwnedRecurringTask(subjectId, recurringTaskId, currentUser);

        recurringTask.setActive(requestDTO.active());

        recurringTaskRepository.save(recurringTask);
        return toResponseDTO(recurringTask);
    }

    // --- Auxiliary methods ---
    private RecurringTask getOwnedRecurringTask(Long subjectId, Long recurringTaskId, User currentUser) {
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new SubjectNotFoundException(subjectId));

        if (!subject.getUser().getId().equals(currentUser.getId())) {
            throw new SubjectNotOwnedException();
        }

        RecurringTask recurringTask = recurringTaskRepository.findById(recurringTaskId)
                .orElseThrow(() -> new RecurringTaskNotFoundException(recurringTaskId));

        if (!recurringTask.getSubject().getId().equals(subjectId)) {
            throw new RecurringTaskNotOwnedException();
        }

        return recurringTask;
    }

    private RecurringTaskResponseDTO toResponseDTO(RecurringTask recurringTask) {
        return RecurringTaskResponseDTO.builder()
                .id(recurringTask.getId())
                .title(recurringTask.getTitle())
                .description(recurringTask.getDescription())
                .frequency(recurringTask.getFrequency())
                .time(recurringTask.getTime())
                .endDate(recurringTask.getEndDate())
                .active(recurringTask.isActive())
                .createdDate(recurringTask.getCreatedDate())
                .latestGeneratedDate(recurringTask.getLatestGeneratedDate())
                .nextOccurrence(computeNextOccurrence(recurringTask))
                .subjectId(recurringTask.getSubject().getId())
                .build();
    }

    // A read-only preview of when this routine will next produce a Task —
    // never mutates latestGeneratedDate itself, that only happens inside
    // RecurringTaskSchedulerService's actual generation run.
    private LocalDateTime computeNextOccurrence(RecurringTask recurringTask) {
        if (!recurringTask.isActive()) {
            return null;
        }

        LocalDate nextDate;
        if (recurringTask.getLatestGeneratedDate() == null) {
            // Never generated yet — the scheduler creates the first
            // occurrence on its very next daily tick regardless of
            // frequency (see RecurringTaskSchedulerService#shouldGenerate).
            nextDate = LocalDate.now().plusDays(1);
        } else {
            nextDate = FrequencyUtils.nextExecution(recurringTask.getFrequency(), recurringTask.getLatestGeneratedDate()).toLocalDate();
        }

        if (recurringTask.getEndDate() != null && nextDate.isAfter(recurringTask.getEndDate())) {
            return null;
        }

        return recurringTask.getTime() != null ? nextDate.atTime(recurringTask.getTime()) : nextDate.atStartOfDay();
    }
}
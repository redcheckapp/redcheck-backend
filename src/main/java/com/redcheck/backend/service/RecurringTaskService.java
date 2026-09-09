package com.redcheck.backend.service;

import com.redcheck.backend.dto.request.RecurringTaskRequestDTO;
import com.redcheck.backend.dto.response.RecurringTaskResponseDTO;
import com.redcheck.backend.dto.update.RecurringTaskActiveDTO;
import com.redcheck.backend.entity.RecurringTask;
import com.redcheck.backend.entity.Subject;
import com.redcheck.backend.entity.Task;
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
        RoutineStats stats = computeStats(recurringTask);
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
                .currentStreak(stats.currentStreak())
                .longestStreak(stats.longestStreak())
                .completionRate(stats.completionRate())
                .totalGenerated(stats.totalGenerated())
                .totalCompleted(stats.totalCompleted())
                .subjectId(recurringTask.getSubject().getId())
                .build();
    }

    private record RoutineStats(int currentStreak, int longestStreak, double completionRate, int totalGenerated, int totalCompleted) {}

    // Streaks/completion rate, derived from the routine's full occurrence
    // history rather than tracked incrementally — a routine's history is
    // always small (at most one Task per day since it began), so O(n) here
    // is cheap and, unlike an incremental counter, can never drift out of
    // sync with reality (e.g. after a past task is manually deleted or its
    // completion is toggled off).
    private RoutineStats computeStats(RecurringTask recurringTask) {
        List<Task> history = taskRepository.findAllByRecurringTaskAndDeletedFalseOrderByAssignedDateAsc(recurringTask);

        int totalGenerated = history.size();
        int totalCompleted = 0;
        int longestStreak = 0;
        int runningStreak = 0;

        for (Task task : history) {
            if (task.getCompletedDate() != null) {
                totalCompleted++;
                runningStreak++;
                longestStreak = Math.max(longestStreak, runningStreak);
            } else {
                runningStreak = 0;
            }
        }

        // Consecutive completed occurrences counting back from the most
        // recent one — deliberately 0 (not "N minus the still-pending one")
        // the instant the latest occurrence isn't completed yet, rather
        // than guessing whether it's "not due yet" vs. "missed". Simple and
        // honest beats a heuristic grace period here.
        int currentStreak = 0;
        for (int i = history.size() - 1; i >= 0; i--) {
            if (history.get(i).getCompletedDate() != null) {
                currentStreak++;
            } else {
                break;
            }
        }

        double completionRate = totalGenerated == 0 ? 0.0 : (double) totalCompleted / totalGenerated;

        return new RoutineStats(currentStreak, longestStreak, completionRate, totalGenerated, totalCompleted);
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
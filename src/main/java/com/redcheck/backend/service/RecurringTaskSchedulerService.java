package com.redcheck.backend.service;

import com.redcheck.backend.entity.RecurringTask;
import com.redcheck.backend.entity.Task;
import com.redcheck.backend.repository.RecurringTaskRepository;
import com.redcheck.backend.repository.TaskRepository;
import com.redcheck.backend.util.FrequencyUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecurringTaskSchedulerService {

    private final RecurringTaskRepository recurringTaskRepository;
    private final TaskRepository taskRepository;

    @Transactional
    @Scheduled(cron = "0 0 0 * * *")
    public void generateTask() {
        log.info("Starting generation of recurring tasks...");
        List<RecurringTask> activeTasks = recurringTaskRepository.findAllByActiveTrue();

        for (RecurringTask recurringTask : activeTasks) {
            // One malformed/unparseable frequency must not abort generation
            // for every other recurring task in this run — isolate each
            // iteration so the rest of the batch still completes.
            try {
                // Safety net: a routine should already be inactive by the day
                // after its endDate (see the post-generation check below),
                // but if a run was ever missed, catch it here too rather than
                // generating one more occurrence past what the user asked
                // for. Strictly *after* endDate — the day of endDate itself
                // is still a valid, final occurrence, handled below.
                if (isAfterEndDate(recurringTask)) {
                    recurringTask.setActive(false);
                    recurringTaskRepository.save(recurringTask);
                    continue;
                }

                if (shouldGenerate(recurringTask)) {
                    Task task = Task.builder()
                            .title(recurringTask.getTitle())
                            .description(recurringTask.getDescription())
                            .deadline(buildDeadline(recurringTask))
                            .subject(recurringTask.getSubject())
                            .recurringTask(recurringTask)
                            .build();

                    taskRepository.save(task);
                    recurringTask.setLatestGeneratedDate(LocalDateTime.now());

                    // If today is on (or somehow past) the routine's last
                    // day, this was its final occurrence — deactivate it now
                    // instead of waiting for tomorrow's run to catch it via
                    // the safety net above.
                    if (hasReachedEndDate(recurringTask)) {
                        recurringTask.setActive(false);
                    }

                    recurringTaskRepository.save(recurringTask);
                }
            } catch (Exception e) {
                log.error("Failed to process recurring task {} (frequency='{}'): {}",
                        recurringTask.getId(), recurringTask.getFrequency(), e.getMessage(), e);
            }
        }
        log.info("Recurring tasks generation completed.");
    }

    private boolean isAfterEndDate(RecurringTask recurringTask) {
        return recurringTask.getEndDate() != null && LocalDate.now().isAfter(recurringTask.getEndDate());
    }

    private boolean hasReachedEndDate(RecurringTask recurringTask) {
        return recurringTask.getEndDate() != null && !LocalDate.now().isBefore(recurringTask.getEndDate());
    }

    private LocalDateTime buildDeadline(RecurringTask recurringTask) {
        return recurringTask.getTime() != null ? LocalDate.now().atTime(recurringTask.getTime()) : null;
    }

    private boolean shouldGenerate(RecurringTask recurringTask) {
        if (recurringTask.getLatestGeneratedDate() == null) {
            return true;
        }

        LocalDateTime next = FrequencyUtils.nextExecution(
                recurringTask.getFrequency(),
                recurringTask.getLatestGeneratedDate()
        );

        if (!LocalDateTime.now().isBefore(next)) {

            // Search for the latest associated task generated
            Optional<Task> lastTask = taskRepository
                    .findTopByRecurringTaskAndDeletedFalseOrderByAssignedDateDesc(recurringTask);

            // If it exists and is not completed, update the generation date without creating a duplicate
            if (lastTask.isPresent() && lastTask.get().getCompletedDate() == null) {
                recurringTask.setLatestGeneratedDate(LocalDateTime.now());
                recurringTaskRepository.save(recurringTask);
                return false;
            }

            return true;
        }

        return false;
    }
}
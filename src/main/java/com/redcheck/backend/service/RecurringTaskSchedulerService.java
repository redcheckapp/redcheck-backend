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
        // First occurrence ever: a simple preset starts on the first tick
        // after creation, but a custom pattern only starts on a day it
        // actually selects — a Mon/Thu routine created on a Tuesday used to
        // get its first task on Wednesday.
        if (recurringTask.getLatestGeneratedDate() == null) {
            LocalDate today = LocalDate.now();
            return FrequencyUtils.firstOccurrenceOnOrAfter(recurringTask.getFrequency(), today).isEqual(today);
        }

        if (!isDueToday(recurringTask)) {
            return false;
        }

        // Today is a due day for this routine's frequency — only generate if
        // the previous occurrence has already been completed (or there is
        // none yet), so a still-pending task from a prior cycle is never
        // silently duplicated.
        Optional<Task> lastTask = taskRepository
                .findTopByRecurringTaskAndDeletedFalseOrderByAssignedDateDesc(recurringTask);

        return lastTask.isEmpty() || lastTask.get().getCompletedDate() != null;
    }

    // Whether the routine's frequency is actually due *today* — not just
    // "has the previously-computed next execution already passed," which
    // would fire on whatever day the scheduler next happens to run rather
    // than a day the frequency actually selects. That distinction only
    // matters for custom frequencies (specific weekdays / a specific day of
    // month): a gap — the routine paused and later reactivated, its
    // frequency edited to a different set of days, or a scheduler run
    // missed (e.g. a deploy around midnight) — used to make the next tick
    // generate immediately regardless of whether today matched the pattern.
    // Walking `next` forward with FrequencyUtils until it's no longer in
    // the past (instead of a single now-vs-next comparison) catches up to
    // the frequency's real next occurrence without ever generating on a day
    // the pattern doesn't select.
    private boolean isDueToday(RecurringTask recurringTask) {
        LocalDate today = LocalDate.now();
        LocalDateTime next = FrequencyUtils.nextExecution(
                recurringTask.getFrequency(),
                recurringTask.getLatestGeneratedDate()
        );

        while (next.toLocalDate().isBefore(today)) {
            next = FrequencyUtils.nextExecution(recurringTask.getFrequency(), next);
        }

        return next.toLocalDate().isEqual(today);
    }
}
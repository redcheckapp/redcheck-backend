package com.redcheck.backend.service;

import com.redcheck.backend.entity.RecurringTask;
import com.redcheck.backend.entity.Task;
import com.redcheck.backend.repository.RecurringTaskRepository;
import com.redcheck.backend.repository.TaskRepository;
import com.redcheck.backend.util.FrequencyUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RecurringTaskSchedulerService {

    private final RecurringTaskRepository recurringTaskRepository;
    private final TaskRepository taskRepository;

    @Transactional
    @Scheduled(cron = "0 0 0 * * *")
    public void generateTask(){
        List<RecurringTask> activeTasks = recurringTaskRepository.findAllByActiveTrue();

        for(RecurringTask recurringTask : activeTasks){
            if(shouldGenerate(recurringTask)){
                Task task = Task.builder()
                        .title(recurringTask.getTitle())
                        .description(recurringTask.getDescription())
                        .subject(recurringTask.getSubject())
                        .recurringTask(recurringTask)
                        .build();

                taskRepository.save(task);
                recurringTask.setLatestGeneratedDate(LocalDateTime.now());
                recurringTaskRepository.save(recurringTask);
            }
        }
    }

    private boolean shouldGenerate(RecurringTask recurringTask) {
        if (recurringTask.getLatestGeneratedDate() == null) return true;

        LocalDateTime next = FrequencyUtils.nextExecution(
                recurringTask.getFrequency(),
                recurringTask.getLatestGeneratedDate()
        );

        if (!LocalDateTime.now().isBefore(next)) {

            // Searches the latest generated task associated
            Optional<Task> lastTask = taskRepository
                    .findTopByRecurringTaskOrderByAssignedDateDesc(recurringTask);

            // If it exists and its not completed, updates the date but not generates
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

package com.redcheck.backend.service;

import com.redcheck.backend.repository.SubjectRepository;
import com.redcheck.backend.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrashCleanupSchedulerService {

    private final TaskRepository taskRepository;
    private final SubjectRepository subjectRepository;

    // Executes in the minute 0 of each hour (ie: 14:00, 15:00, 16:00...)
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cleanupExpiredTrash() {
        // Calculates deadline: 24 hours ago
        LocalDateTime cutoffDate = LocalDateTime.now().minusHours(24);

        log.info("Initiating recycle bin clean up for elements prior to: {}", cutoffDate);

        // Removes the tasks in the bin
        taskRepository.deleteTrashOlderThan(cutoffDate);

        // Removes the subjects in the bin (and all their associated info <- CASCADE)
        subjectRepository.deleteTrashOlderThan(cutoffDate);

        log.info("Recycle bin cleaning up ended up successfully.");
    }
}
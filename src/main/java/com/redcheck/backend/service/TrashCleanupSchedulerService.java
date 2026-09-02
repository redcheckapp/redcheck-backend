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

    // Executes at minute 0 of every hour (e.g., 14:00, 15:00, 16:00...)
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cleanupExpiredTrash() {
        // Calculate the cutoff date: 24 hours ago
        LocalDateTime cutoffDate = LocalDateTime.now().minusHours(24);

        log.info("Initiating recycle bin cleanup for elements prior to: {}", cutoffDate);

        // Remove soft-deleted tasks older than the cutoff date
        taskRepository.deleteTrashOlderThan(cutoffDate);

        // Remove soft-deleted subjects older than the cutoff date (and cascaded contents)
        subjectRepository.deleteTrashOlderThan(cutoffDate);

        log.info("Recycle bin cleanup completed successfully.");
    }
}
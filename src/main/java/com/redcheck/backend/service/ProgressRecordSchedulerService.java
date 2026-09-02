package com.redcheck.backend.service;

import com.redcheck.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProgressRecordSchedulerService {

    private final UserRepository userRepository;
    private final ProgressRecordService progressRecordService;

    @Scheduled(cron = "0 59 23 * * *")
    public void generateDailyProgressRecords() {
        log.info("Starting generation of daily progress records for all users...");
        userRepository.findAll().forEach(progressRecordService::generateDailyRecord);
        log.info("Daily progress records generation completed.");
    }
}
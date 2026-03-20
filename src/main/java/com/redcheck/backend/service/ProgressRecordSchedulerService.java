package com.redcheck.backend.service;

import com.redcheck.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProgressRecordSchedulerService {

    private final UserRepository userRepository;
    private final ProgressRecordService progressRecordService;

    @Scheduled(cron = "0 59 23 * * *")
    public void generateDailyProgressRecords(){
        userRepository.findAll().forEach(progressRecordService::generateDailyRecord);
    }
}

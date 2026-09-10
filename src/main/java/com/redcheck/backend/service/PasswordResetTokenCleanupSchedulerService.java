package com.redcheck.backend.service;

import com.redcheck.backend.repository.PasswordResetTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

// Same hourly-sweep pattern as TrashCleanupSchedulerService — expired or
// already-used reset tokens have no reason to linger in the table.
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetTokenCleanupSchedulerService {

    private final PasswordResetTokenRepository passwordResetTokenRepository;

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cleanupExpiredTokens() {
        log.info("Cleaning up expired/used password reset tokens.");
        passwordResetTokenRepository.deleteExpiredOrUsed(LocalDateTime.now());
    }
}

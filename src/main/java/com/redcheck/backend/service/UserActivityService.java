package com.redcheck.backend.service;

import com.redcheck.backend.entity.User;
import com.redcheck.backend.event.UserSeenEvent;
import com.redcheck.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserActivityService {

    // Active-user counts only need day-level precision, so one write per
    // user per hour is plenty — the check below is in-memory against the
    // User the filter just loaded, so most requests cost nothing extra.
    static final Duration REFRESH_INTERVAL = Duration.ofHours(1);

    private final UserRepository userRepository;

    @EventListener
    @Transactional
    public void onUserSeen(UserSeenEvent event) {
        User user = event.user();
        LocalDateTime now = LocalDateTime.now();
        if (user.getLastSeenAt() != null && user.getLastSeenAt().isAfter(now.minus(REFRESH_INTERVAL))) {
            return;
        }
        // Never let bookkeeping fail the actual request.
        try {
            userRepository.updateLastSeenAt(user.getId(), now);
        } catch (Exception e) {
            log.warn("Failed to update lastSeenAt for user {}", user.getId(), e);
        }
    }
}

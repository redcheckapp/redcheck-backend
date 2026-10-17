package com.redcheck.backend.service;

import com.redcheck.backend.dto.response.UserStatsResponseDTO;
import com.redcheck.backend.entity.User;
import com.redcheck.backend.repository.TaskRepository;
import com.redcheck.backend.repository.UserRepository;
import com.redcheck.backend.util.DemoAccountUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class UserStatsService {

    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final AdminAccessService adminAccessService;

    // Aggregated in memory, same reasoning as AiUsageService#getStats: the
    // user base and a 90-day activity window are small, and it keeps every
    // query DB-agnostic.
    @Transactional(readOnly = true)
    public UserStatsResponseDTO getStats(int days) {
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        LocalDate firstDay = today.minusDays(days - 1L);

        // Demo accounts are shared by anyone trying the app and the admins
        // are the developer — both would inflate every number.
        List<User> users = userRepository.findAll().stream()
                .filter(user -> !DemoAccountUtils.isDemoAccount(user.getEmail()) && !adminAccessService.isAdmin(user))
                .toList();
        Set<Long> countedIds = users.stream().map(User::getId).collect(Collectors.toSet());

        // Activity: one set of user ids per day. The window always covers at
        // least 30 days so acting7d/acting30d don't depend on `days`.
        LocalDateTime activityFrom = today.minusDays(Math.max(days, 30) - 1L).atStartOfDay();
        Map<LocalDate, Set<Long>> actingByDay = new HashMap<>();
        Stream.concat(
                taskRepository.findCompletionActivitySince(activityFrom).stream(),
                taskRepository.findManualCreationActivitySince(activityFrom).stream()
        ).forEach(row -> {
            Long userId = (Long) row[0];
            if (countedIds.contains(userId)) {
                LocalDate day = ((LocalDateTime) row[1]).toLocalDate();
                actingByDay.computeIfAbsent(day, d -> new HashSet<>()).add(userId);
            }
        });

        Map<LocalDate, Long> signupsByDay = users.stream()
                .filter(user -> user.getCreationDate() != null && !user.getCreationDate().toLocalDate().isBefore(firstDay))
                .collect(Collectors.groupingBy(user -> user.getCreationDate().toLocalDate(), Collectors.counting()));

        List<UserStatsResponseDTO.DailyUsers> daily = new ArrayList<>();
        for (LocalDate day = firstDay; !day.isAfter(today); day = day.plusDays(1)) {
            daily.add(UserStatsResponseDTO.DailyUsers.builder()
                    .date(day)
                    .actingUsers(actingByDay.getOrDefault(day, Set.of()).size())
                    .signups(signupsByDay.getOrDefault(day, 0L))
                    .build());
        }

        return UserStatsResponseDTO.builder()
                .days(days)
                .totalUsers(users.size())
                .googleUsers(users.stream().filter(user -> user.getGoogleId() != null).count())
                .newUsers(signupsByDay.values().stream().mapToLong(Long::longValue).sum())
                .seenToday(users.stream().filter(user -> user.getLastSeenAt() != null
                        && user.getLastSeenAt().toLocalDate().isEqual(today)).count())
                .seen7d(countSeenSince(users, now.minusDays(7)))
                .seen30d(countSeenSince(users, now.minusDays(30)))
                .acting7d(countActingSince(actingByDay, today.minusDays(6)))
                .acting30d(countActingSince(actingByDay, today.minusDays(29)))
                .daily(daily)
                .build();
    }

    private long countSeenSince(List<User> users, LocalDateTime since) {
        return users.stream().filter(user -> user.getLastSeenAt() != null && !user.getLastSeenAt().isBefore(since)).count();
    }

    // Distinct users across every day from `since` through today.
    private long countActingSince(Map<LocalDate, Set<Long>> actingByDay, LocalDate since) {
        return actingByDay.entrySet().stream()
                .filter(entry -> !entry.getKey().isBefore(since))
                .flatMap(entry -> entry.getValue().stream())
                .distinct()
                .count();
    }
}

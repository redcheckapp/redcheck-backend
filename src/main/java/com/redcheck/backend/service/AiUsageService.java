package com.redcheck.backend.service;

import com.redcheck.backend.dto.response.AiUsageStatsResponseDTO;
import com.redcheck.backend.entity.AiUsageLog;
import com.redcheck.backend.entity.User;
import com.redcheck.backend.repository.AiUsageLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiUsageService {

    private final AiUsageLogRepository aiUsageLogRepository;

    // Called from SmartCheckAIService around each engine call. Its own
    // transaction (REQUIRES_NEW) and its own try/catch so that failing to
    // write a usage row can never roll back or break the AI flow it's
    // measuring — losing one log row is fine, losing a user's analysis isn't.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(AiUsageLog.Type type, boolean success, long durationMs, int taskCount, User user) {
        try {
            aiUsageLogRepository.save(AiUsageLog.builder()
                    .type(type)
                    .success(success)
                    .durationMs(durationMs)
                    .taskCount(taskCount)
                    .user(user)
                    .build());
        } catch (Exception e) {
            log.error("Failed to record AI usage ({}, success={})", type, success, e);
        }
    }

    // Aggregated in memory rather than with GROUP BY DATE(...): it keeps the
    // query DB-agnostic and the window is small (a few thousand rows at most).
    @Transactional(readOnly = true)
    public AiUsageStatsResponseDTO getStats(int days) {
        LocalDate today = LocalDate.now();
        LocalDate firstDay = today.minusDays(days - 1L);
        List<AiUsageLog> logs = aiUsageLogRepository.findAllByCreatedDateGreaterThanEqual(firstDay.atStartOfDay());

        Map<LocalDate, List<AiUsageLog>> byDay = logs.stream()
                .collect(Collectors.groupingBy(entry -> entry.getCreatedDate().toLocalDate()));

        List<AiUsageStatsResponseDTO.DailyUsage> daily = new ArrayList<>();
        for (LocalDate day = firstDay; !day.isAfter(today); day = day.plusDays(1)) {
            List<AiUsageLog> dayLogs = byDay.getOrDefault(day, List.of());
            daily.add(AiUsageStatsResponseDTO.DailyUsage.builder()
                    .date(day)
                    .prioritizeCalls(countOfType(dayLogs, AiUsageLog.Type.PRIORITIZE))
                    .outcomeCalls(countOfType(dayLogs, AiUsageLog.Type.TASK_OUTCOME))
                    .failedCalls(dayLogs.stream().filter(entry -> !entry.isSuccess()).count())
                    .build());
        }

        long avgPrioritizeDurationMs = Math.round(logs.stream()
                .filter(entry -> entry.getType() == AiUsageLog.Type.PRIORITIZE && entry.isSuccess())
                .mapToLong(AiUsageLog::getDurationMs)
                .average()
                .orElse(0));

        // Users who ran at least one analysis. TASK_OUTCOME rows carry no user.
        long distinctUsers = logs.stream()
                .filter(entry -> entry.getType() == AiUsageLog.Type.PRIORITIZE)
                .map(AiUsageLog::getUser)
                .filter(Objects::nonNull)
                .map(User::getId)
                .distinct()
                .count();

        return AiUsageStatsResponseDTO.builder()
                .days(days)
                .todayPrioritizeCalls(daily.get(daily.size() - 1).prioritizeCalls())
                .totalPrioritizeCalls(countOfType(logs, AiUsageLog.Type.PRIORITIZE))
                .totalOutcomeCalls(countOfType(logs, AiUsageLog.Type.TASK_OUTCOME))
                .failedCalls(logs.stream().filter(entry -> !entry.isSuccess()).count())
                .avgPrioritizeDurationMs(avgPrioritizeDurationMs)
                .distinctUsers(distinctUsers)
                .daily(daily)
                .build();
    }

    private long countOfType(List<AiUsageLog> logs, AiUsageLog.Type type) {
        return logs.stream().filter(entry -> entry.getType() == type).count();
    }
}

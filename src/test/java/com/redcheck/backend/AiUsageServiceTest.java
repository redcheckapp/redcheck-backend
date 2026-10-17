package com.redcheck.backend;

import com.redcheck.backend.dto.response.AiUsageStatsResponseDTO;
import com.redcheck.backend.entity.AiUsageLog;
import com.redcheck.backend.entity.User;
import com.redcheck.backend.repository.AiUsageLogRepository;
import com.redcheck.backend.service.AiUsageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unitary Tests - AiUsageService")
public class AiUsageServiceTest {

    @Mock
    private AiUsageLogRepository aiUsageLogRepository;

    @InjectMocks
    private AiUsageService aiUsageService;

    private static AiUsageLog log(AiUsageLog.Type type, boolean success, long durationMs,
                                  LocalDateTime createdDate, User user) {
        AiUsageLog entry = AiUsageLog.builder()
                .type(type).success(success).durationMs(durationMs).taskCount(1).user(user).build();
        entry.setCreatedDate(createdDate);
        return entry;
    }

    @Nested
    @DisplayName("Method: record")
    class RecordTests {

        @Test
        @DisplayName("Should save a usage row with the given values")
        void record_ShouldSaveRow() {
            // WHEN
            aiUsageService.record(AiUsageLog.Type.PRIORITIZE, true, 1200, 5, null);

            // THEN
            ArgumentCaptor<AiUsageLog> captor = ArgumentCaptor.forClass(AiUsageLog.class);
            verify(aiUsageLogRepository).save(captor.capture());
            assertEquals(AiUsageLog.Type.PRIORITIZE, captor.getValue().getType());
            assertTrue(captor.getValue().isSuccess());
            assertEquals(1200, captor.getValue().getDurationMs());
            assertEquals(5, captor.getValue().getTaskCount());
        }

        @Test
        @DisplayName("A failing save should be swallowed, never propagated to the AI flow")
        void record_WhenSaveFails_ShouldNotThrow() {
            // GIVEN
            when(aiUsageLogRepository.save(any())).thenThrow(new RuntimeException("DB down"));

            // WHEN & THEN
            assertDoesNotThrow(() -> aiUsageService.record(AiUsageLog.Type.TASK_OUTCOME, false, 10, 1, null));
        }
    }

    @Nested
    @DisplayName("Method: getStats")
    class GetStatsTests {

        @Test
        @DisplayName("Should aggregate totals and fill every day of the window, zeros included")
        void getStats_ShouldAggregatePerDay() {
            // GIVEN
            User alice = User.builder().username("alice").email("a@redcheck.com").build();
            alice.setId(1L);
            User bob = User.builder().username("bob").email("b@redcheck.com").build();
            bob.setId(2L);
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime twoDaysAgo = now.minusDays(2);
            when(aiUsageLogRepository.findAllByCreatedDateGreaterThanEqual(any())).thenReturn(List.of(
                    log(AiUsageLog.Type.PRIORITIZE, true, 1000, now, alice),
                    log(AiUsageLog.Type.PRIORITIZE, true, 3000, now, alice),
                    log(AiUsageLog.Type.PRIORITIZE, false, 9000, twoDaysAgo, bob),
                    log(AiUsageLog.Type.TASK_OUTCOME, true, 50, twoDaysAgo, null)
            ));

            // WHEN
            AiUsageStatsResponseDTO stats = aiUsageService.getStats(7);

            // THEN
            assertEquals(7, stats.daily().size());
            assertEquals(LocalDate.now(), stats.daily().get(6).date());
            assertEquals(2, stats.todayPrioritizeCalls());
            assertEquals(3, stats.totalPrioritizeCalls());
            assertEquals(1, stats.totalOutcomeCalls());
            assertEquals(1, stats.failedCalls());
            // Failed calls don't count towards the average duration.
            assertEquals(2000, stats.avgPrioritizeDurationMs());
            assertEquals(2, stats.distinctUsers());

            AiUsageStatsResponseDTO.DailyUsage day = stats.daily().get(4);
            assertEquals(twoDaysAgo.toLocalDate(), day.date());
            assertEquals(1, day.prioritizeCalls());
            assertEquals(1, day.outcomeCalls());
            assertEquals(1, day.failedCalls());
            assertEquals(0, stats.daily().get(5).prioritizeCalls());
        }
    }
}

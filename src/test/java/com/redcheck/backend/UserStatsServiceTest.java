package com.redcheck.backend;

import com.redcheck.backend.dto.response.UserStatsResponseDTO;
import com.redcheck.backend.entity.User;
import com.redcheck.backend.repository.TaskRepository;
import com.redcheck.backend.repository.UserRepository;
import com.redcheck.backend.service.AdminAccessService;
import com.redcheck.backend.service.UserStatsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unitary Tests - UserStatsService")
public class UserStatsServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private AdminAccessService adminAccessService;

    @InjectMocks
    private UserStatsService userStatsService;

    private final LocalDateTime now = LocalDateTime.now();

    private static User user(long id, String email, LocalDateTime createdAt, LocalDateTime lastSeenAt, String googleId) {
        User user = User.builder().username("u" + id).email(email).creationDate(createdAt)
                .lastSeenAt(lastSeenAt).googleId(googleId).build();
        user.setId(id);
        return user;
    }

    @BeforeEach
    void setUp() {
        // Decided by email: User's equals() treats every instance as equal,
        // and a strict argThat stub would reject the non-admin calls.
        when(adminAccessService.isAdmin(any(User.class)))
                .thenAnswer(inv -> "dev@redcheck.com".equals(inv.<User>getArgument(0).getEmail()));
    }

    @Test
    @DisplayName("Should count real users only, with both notions of active")
    void getStats_ShouldExcludeDemoAndAdminAndCountActivity() {
        // GIVEN
        // "now" rather than "a few hours ago" for today's events, so the
        // test can't straddle midnight.
        User ana = user(1, "ana@example.com", now.minusDays(2), now, "google-ana");
        User bob = user(2, "bob@example.com", now.minusDays(60), now.minusDays(10), null);
        User carla = user(3, "carla@example.com", now.minusDays(90), null, null);
        User demo = user(4, "demo-es@redcheck.com", now.minusDays(1), now, null);
        User admin = user(5, "dev@redcheck.com", now.minusDays(100), now, null);
        when(userRepository.findAll()).thenReturn(List.of(ana, bob, carla, demo, admin));

        when(taskRepository.findCompletionActivitySince(any())).thenReturn(List.of(
                new Object[]{1L, now},                         // ana, today
                new Object[]{2L, now.minusDays(12)},           // bob, within 30d only
                new Object[]{4L, now.minusHours(1)},           // demo: ignored
                new Object[]{5L, now.minusHours(1)}            // admin: ignored
        ));
        when(taskRepository.findManualCreationActivitySince(any())).thenReturn(List.<Object[]>of(
                new Object[]{1L, now.minusDays(1)}             // ana again, yesterday
        ));

        // WHEN
        UserStatsResponseDTO stats = userStatsService.getStats(7);

        // THEN
        assertEquals(3, stats.totalUsers());
        assertEquals(1, stats.googleUsers());
        assertEquals(1, stats.newUsers());          // ana, within the 7-day window
        assertEquals(1, stats.seenToday());          // ana
        assertEquals(1, stats.seen7d());
        assertEquals(2, stats.seen30d());            // ana + bob
        assertEquals(1, stats.acting7d());           // ana
        assertEquals(2, stats.acting30d());          // ana + bob

        assertEquals(7, stats.daily().size());
        UserStatsResponseDTO.DailyUsers today = stats.daily().get(6);
        assertEquals(LocalDate.now(), today.date());
        assertEquals(1, today.actingUsers());
    }
}

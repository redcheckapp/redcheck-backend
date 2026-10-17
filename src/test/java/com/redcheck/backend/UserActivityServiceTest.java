package com.redcheck.backend;

import com.redcheck.backend.entity.User;
import com.redcheck.backend.event.UserSeenEvent;
import com.redcheck.backend.repository.UserRepository;
import com.redcheck.backend.service.UserActivityService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unitary Tests - UserActivityService")
public class UserActivityServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserActivityService userActivityService;

    private static User userSeenAt(LocalDateTime lastSeenAt) {
        User user = User.builder().username("user").email("user@redcheck.com").lastSeenAt(lastSeenAt).build();
        user.setId(1L);
        return user;
    }

    @Test
    @DisplayName("Never seen before should record lastSeenAt")
    void onUserSeen_WhenNeverSeen_ShouldUpdate() {
        userActivityService.onUserSeen(new UserSeenEvent(userSeenAt(null)));

        verify(userRepository).updateLastSeenAt(eq(1L), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("Seen over an hour ago should refresh lastSeenAt")
    void onUserSeen_WhenStale_ShouldUpdate() {
        userActivityService.onUserSeen(new UserSeenEvent(userSeenAt(LocalDateTime.now().minusHours(2))));

        verify(userRepository).updateLastSeenAt(eq(1L), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("Seen within the last hour should not write at all")
    void onUserSeen_WhenFresh_ShouldSkip() {
        userActivityService.onUserSeen(new UserSeenEvent(userSeenAt(LocalDateTime.now().minusMinutes(10))));

        verify(userRepository, never()).updateLastSeenAt(anyLong(), any());
    }

    @Test
    @DisplayName("A failing write should never propagate to the request")
    void onUserSeen_WhenWriteFails_ShouldNotThrow() {
        when(userRepository.updateLastSeenAt(anyLong(), any())).thenThrow(new RuntimeException("DB down"));

        assertDoesNotThrow(() -> userActivityService.onUserSeen(new UserSeenEvent(userSeenAt(null))));
    }
}

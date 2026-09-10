package com.redcheck.backend;

import com.redcheck.backend.entity.Task;
import com.redcheck.backend.entity.User;
import com.redcheck.backend.repository.AiResponseRepository;
import com.redcheck.backend.repository.NotificationRepository;
import com.redcheck.backend.repository.TaskRepository;
import com.redcheck.backend.service.SmartCheckAIService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unitary Tests - SmartCheckAIService")
public class SmartCheckAIServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private AiResponseRepository aiResponseRepository;

    @InjectMocks
    private SmartCheckAIService smartCheckAIService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .username("user")
                .email("user@redcheck.com")
                .password("redcheckUser")
                .build();
        user.setId(1L);
    }

    @Nested
    @DisplayName("Method: hasPendingTasks")
    class HasPendingTasksTests {

        @Test
        @DisplayName("When the user has at least one pending task should return true")
        void hasPendingTasks_WhenUserHasPendingTasks_ShouldReturnTrue() {
            // GIVEN
            Task pendingTask = Task.builder().title("Pending task").build();
            when(taskRepository.findAllBySubject_User_IdAndCompletedDateIsNullAndDeletedFalse(user.getId()))
                    .thenReturn(List.of(pendingTask));

            // WHEN
            boolean result = smartCheckAIService.hasPendingTasks(user);

            // THEN
            assertTrue(result);
        }

        @Test
        @DisplayName("When the user has no pending tasks should return false")
        void hasPendingTasks_WhenUserHasNoPendingTasks_ShouldReturnFalse() {
            // GIVEN
            when(taskRepository.findAllBySubject_User_IdAndCompletedDateIsNullAndDeletedFalse(user.getId()))
                    .thenReturn(Collections.emptyList());

            // WHEN
            boolean result = smartCheckAIService.hasPendingTasks(user);

            // THEN
            assertFalse(result);
        }
    }
}

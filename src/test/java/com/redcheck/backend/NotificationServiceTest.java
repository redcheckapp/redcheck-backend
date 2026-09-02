package com.redcheck.backend;

import com.redcheck.backend.dto.response.NotificationResponseDTO;
import com.redcheck.backend.entity.Notification;
import com.redcheck.backend.entity.User;
import com.redcheck.backend.repository.NotificationRepository;
import com.redcheck.backend.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unitary Tests - NotificationService")
public class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    private User user;
    private Notification mockNotification;
    private LocalDateTime testDate;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .username("user")
                .email("user@redcheck.com")
                .password("redcheckUser")
                .build();
        user.setId(1L);

        testDate = LocalDateTime.now();

        mockNotification = Notification.builder()
                .title("Test Notification")
                .message("This is a test notification message")
                .read(false)
                .creationDate(testDate)
                .user(user)
                .build();
        mockNotification.setId(10L);
    }

    @Nested
    @DisplayName("Method: getNotifications")
    class GetNotificationsTests {

        @Test
        @DisplayName("When 'read' filter is applied should return filtered mapped notifications")
        void getNotifications_WhenReadFilterApplied_ShouldReturnFilteredMappedNotifications() {
            // GIVEN
            Boolean readFilter = false;
            when(notificationRepository.findAllByUserAndRead(user, readFilter))
                    .thenReturn(Collections.singletonList(mockNotification));

            // WHEN
            List<NotificationResponseDTO> result = notificationService.getNotifications(user, readFilter);

            // THEN
            assertFalse(result.isEmpty());
            assertEquals(1, result.size());

            NotificationResponseDTO dto = result.get(0);
            assertEquals(mockNotification.getId(), dto.id());
            assertEquals(mockNotification.getTitle(), dto.title());
            assertEquals(mockNotification.getMessage(), dto.message());
            assertFalse(dto.read());
            assertEquals(mockNotification.getCreationDate(), dto.creationDate()); // Verifica el mapeo privado toResponseDTO
            verify(notificationRepository, times(1)).findAllByUserAndRead(user, readFilter);
            verify(notificationRepository, never()).findAllByUser(any());
        }

        @Test
        @DisplayName("When 'read' filter is null should return all mapped notifications")
        void getNotifications_WhenReadFilterIsNull_ShouldReturnAllMappedNotifications() {
            // GIVEN
            when(notificationRepository.findAllByUser(user))
                    .thenReturn(Collections.singletonList(mockNotification));

            // WHEN
            List<NotificationResponseDTO> result = notificationService.getNotifications(user, null);

            // THEN
            assertFalse(result.isEmpty());
            assertEquals(1, result.size());

            NotificationResponseDTO dto = result.get(0);
            assertEquals(mockNotification.getId(), dto.id());
            assertEquals(mockNotification.getTitle(), dto.title());
            verify(notificationRepository, times(1)).findAllByUser(user);
            verify(notificationRepository, never()).findAllByUserAndRead(any(User.class), anyBoolean());
        }
    }
}
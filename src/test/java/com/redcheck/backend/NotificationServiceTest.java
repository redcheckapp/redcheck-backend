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
import com.redcheck.backend.exception.NotificationNotFoundException;
import java.util.Optional;

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

    @Nested
    @DisplayName("Method: getNotificationsByType")
    class GetNotificationsByTypeTests {

        @Test
        @DisplayName("Should filter by type and read state, including the type in the DTO")
        void getNotificationsByType_ShouldFilterAndMapType() {
            // GIVEN
            mockNotification.setType(Notification.Type.FEEDBACK_REPLY);
            when(notificationRepository.findAllByUserAndReadAndTypeOrderByCreationDateDesc(user, false, Notification.Type.FEEDBACK_REPLY))
                    .thenReturn(List.of(mockNotification));

            // WHEN
            List<NotificationResponseDTO> result = notificationService.getNotificationsByType(user, false, Notification.Type.FEEDBACK_REPLY);

            // THEN
            assertEquals(1, result.size());
            assertEquals(Notification.Type.FEEDBACK_REPLY, result.get(0).type());
        }
    }

    @Nested
    @DisplayName("Method: markAsRead")
    class MarkAsReadTests {

        @Test
        @DisplayName("Should mark the user's own notification as read")
        void markAsRead_ForOwnNotification_ShouldSetRead() {
            // GIVEN
            when(notificationRepository.findByIdAndUser(10L, user)).thenReturn(Optional.of(mockNotification));

            // WHEN
            notificationService.markAsRead(user, 10L);

            // THEN
            assertTrue(mockNotification.isRead());
            verify(notificationRepository).save(mockNotification);
        }

        @Test
        @DisplayName("Should throw for a missing or someone else's notification")
        void markAsRead_WhenNotOwned_ShouldThrow() {
            // GIVEN
            when(notificationRepository.findByIdAndUser(99L, user)).thenReturn(Optional.empty());

            // WHEN & THEN
            assertThrows(NotificationNotFoundException.class, () -> notificationService.markAsRead(user, 99L));
            verify(notificationRepository, never()).save(any());
        }
    }
}

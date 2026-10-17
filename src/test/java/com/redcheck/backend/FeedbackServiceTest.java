package com.redcheck.backend;

import com.redcheck.backend.dto.request.FeedbackReplyRequestDTO;
import com.redcheck.backend.dto.request.FeedbackRequestDTO;
import com.redcheck.backend.dto.response.AdminFeedbackResponseDTO;
import com.redcheck.backend.dto.response.FeedbackResponseDTO;
import com.redcheck.backend.dto.update.AdminFeedbackUpdateDTO;
import com.redcheck.backend.entity.Feedback;
import com.redcheck.backend.entity.Notification;
import com.redcheck.backend.entity.User;
import com.redcheck.backend.exception.FeedbackAuthorDeletedException;
import com.redcheck.backend.exception.FeedbackNotFoundException;
import com.redcheck.backend.repository.FeedbackRepository;
import com.redcheck.backend.repository.NotificationRepository;
import com.redcheck.backend.service.AdminAccessService;
import com.redcheck.backend.service.EmailService;
import com.redcheck.backend.service.FeedbackService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unitary Tests - FeedbackService")
public class FeedbackServiceTest {

    @Mock
    private FeedbackRepository feedbackRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private AdminAccessService adminAccessService;

    @InjectMocks
    private FeedbackService feedbackService;

    private User user;
    private FeedbackRequestDTO mockRequestDTO;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .username("user")
                .email("user@redcheck.com")
                .password("redcheckUser")
                .build();
        user.setId(1L);

        mockRequestDTO = FeedbackRequestDTO.builder()
                .category(Feedback.Category.BUG)
                .message("Something is broken")
                .build();
    }

    @Nested
    @DisplayName("Method: createFeedback")
    class CreateFeedbackTests {

        @Test
        @DisplayName("Should save and return the feedback")
        void createFeedback_ShouldSaveAndReturnFeedback() {
            // WHEN
            FeedbackResponseDTO result = feedbackService.createFeedback(mockRequestDTO, user);

            // THEN
            assertNotNull(result);
            assertEquals(Feedback.Category.BUG, result.category());
            assertEquals("Something is broken", result.message());
            verify(feedbackRepository, times(1)).save(any(Feedback.class));
        }

        @Test
        @DisplayName("Should associate the feedback with the current user")
        void createFeedback_ShouldAssociateWithCurrentUser() {
            // GIVEN
            org.mockito.ArgumentCaptor<Feedback> captor = org.mockito.ArgumentCaptor.forClass(Feedback.class);

            // WHEN
            feedbackService.createFeedback(mockRequestDTO, user);

            // THEN
            verify(feedbackRepository).save(captor.capture());
            assertEquals(user, captor.getValue().getUser());
        }

        @Test
        @DisplayName("Should notify the admins by email with the feedback's content")
        void createFeedback_ShouldNotifyAdmins() {
            // GIVEN
            when(adminAccessService.getAdminEmails()).thenReturn(Set.of("dev@redcheck.com"));

            // WHEN
            feedbackService.createFeedback(mockRequestDTO, user);

            // THEN
            verify(emailService).sendNewFeedbackNotification(
                    Set.of("dev@redcheck.com"), "BUG", "Something is broken", "user@redcheck.com");
        }
    }

    @Nested
    @DisplayName("Method: getAllFeedbackForAdmin")
    class GetAllFeedbackForAdminTests {

        @Test
        @DisplayName("Should map the author's email and alias, or null once the author is gone")
        void getAllFeedbackForAdmin_ShouldMapAuthor() {
            // GIVEN
            Feedback withAuthor = Feedback.builder().id(1L).category(Feedback.Category.BUG)
                    .message("Broken").user(user).build();
            Feedback orphaned = Feedback.builder().id(2L).category(Feedback.Category.OTHER)
                    .message("Hi").build();
            when(feedbackRepository.findAllByOrderByCreatedDateDesc()).thenReturn(List.of(withAuthor, orphaned));

            // WHEN
            List<AdminFeedbackResponseDTO> result = feedbackService.getAllFeedbackForAdmin();

            // THEN
            assertEquals(2, result.size());
            assertEquals("user@redcheck.com", result.get(0).userEmail());
            assertEquals("user", result.get(0).userAlias());
            assertEquals(Feedback.Status.NEW, result.get(0).status());
            assertNull(result.get(1).userEmail());
            assertNull(result.get(1).userAlias());
        }
    }

    @Nested
    @DisplayName("Method: updateFeedbackForAdmin")
    class UpdateFeedbackForAdminTests {

        @Test
        @DisplayName("Should update only the fields that are present")
        void updateFeedbackForAdmin_ShouldApplyPartialUpdate() {
            // GIVEN
            Feedback feedback = Feedback.builder().id(1L).category(Feedback.Category.BUG)
                    .message("Broken").adminNote("Old note").build();
            when(feedbackRepository.findById(1L)).thenReturn(Optional.of(feedback));

            // WHEN
            AdminFeedbackResponseDTO result = feedbackService.updateFeedbackForAdmin(1L,
                    AdminFeedbackUpdateDTO.builder().status(Feedback.Status.DONE).build());

            // THEN
            assertEquals(Feedback.Status.DONE, result.status());
            assertEquals("Old note", result.adminNote());
            verify(feedbackRepository).save(feedback);
        }

        @Test
        @DisplayName("A blank note should clear it")
        void updateFeedbackForAdmin_WithBlankNote_ShouldClearIt() {
            // GIVEN
            Feedback feedback = Feedback.builder().id(1L).category(Feedback.Category.BUG)
                    .message("Broken").adminNote("Old note").build();
            when(feedbackRepository.findById(1L)).thenReturn(Optional.of(feedback));

            // WHEN
            AdminFeedbackResponseDTO result = feedbackService.updateFeedbackForAdmin(1L,
                    AdminFeedbackUpdateDTO.builder().adminNote("  ").build());

            // THEN
            assertNull(result.adminNote());
            assertEquals(Feedback.Status.NEW, result.status());
        }

        @Test
        @DisplayName("Should throw when the feedback doesn't exist")
        void updateFeedbackForAdmin_WhenNotFound_ShouldThrow() {
            // GIVEN
            when(feedbackRepository.findById(99L)).thenReturn(Optional.empty());

            // WHEN & THEN
            assertThrows(FeedbackNotFoundException.class, () -> feedbackService.updateFeedbackForAdmin(99L,
                    AdminFeedbackUpdateDTO.builder().status(Feedback.Status.SEEN).build()));
            verify(feedbackRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Method: notifyAuthor")
    class NotifyAuthorTests {

        @Test
        @DisplayName("Should create a FEEDBACK_REPLY notification for the author and stamp the date")
        void notifyAuthor_ShouldNotifyAndStampDate() {
            // GIVEN
            Feedback feedback = Feedback.builder().id(1L).category(Feedback.Category.SUGGESTION)
                    .message("Dark mode please").user(user).build();
            when(feedbackRepository.findById(1L)).thenReturn(Optional.of(feedback));

            // WHEN
            AdminFeedbackResponseDTO result = feedbackService.notifyAuthor(1L,
                    FeedbackReplyRequestDTO.builder().message("  It's live!  ").build());

            // THEN
            org.mockito.ArgumentCaptor<Notification> captor = org.mockito.ArgumentCaptor.forClass(Notification.class);
            verify(notificationRepository).save(captor.capture());
            assertEquals(Notification.Type.FEEDBACK_REPLY, captor.getValue().getType());
            assertEquals("It's live!", captor.getValue().getMessage());
            assertEquals(user, captor.getValue().getUser());
            assertNotNull(result.userNotifiedDate());
        }

        @Test
        @DisplayName("Should email the same reply only when sendEmail is set")
        void notifyAuthor_WithSendEmail_ShouldAlsoEmail() {
            // GIVEN
            Feedback feedback = Feedback.builder().id(1L).category(Feedback.Category.SUGGESTION)
                    .message("Dark mode please").user(user).build();
            when(feedbackRepository.findById(1L)).thenReturn(Optional.of(feedback));

            // WHEN
            feedbackService.notifyAuthor(1L, FeedbackReplyRequestDTO.builder()
                    .message("It's live!").sendEmail(true).lang("en").build());
            feedbackService.notifyAuthor(1L, FeedbackReplyRequestDTO.builder()
                    .message("Again").sendEmail(false).build());

            // THEN
            verify(emailService, times(1)).sendFeedbackReplyEmail(
                    "user@redcheck.com", "It's live!", "Dark mode please", "en");
        }

        @Test
        @DisplayName("Should refuse when the author deleted their account")
        void notifyAuthor_WhenAuthorDeleted_ShouldThrow() {
            // GIVEN
            Feedback orphaned = Feedback.builder().id(2L).category(Feedback.Category.BUG).message("x").build();
            when(feedbackRepository.findById(2L)).thenReturn(Optional.of(orphaned));

            // WHEN & THEN
            assertThrows(FeedbackAuthorDeletedException.class, () -> feedbackService.notifyAuthor(2L,
                    FeedbackReplyRequestDTO.builder().message("Thanks").build()));
            verify(notificationRepository, never()).save(any());
        }
    }
}

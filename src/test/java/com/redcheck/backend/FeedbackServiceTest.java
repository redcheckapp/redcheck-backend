package com.redcheck.backend;

import com.redcheck.backend.dto.request.FeedbackRequestDTO;
import com.redcheck.backend.dto.response.FeedbackResponseDTO;
import com.redcheck.backend.entity.Feedback;
import com.redcheck.backend.entity.User;
import com.redcheck.backend.repository.FeedbackRepository;
import com.redcheck.backend.service.FeedbackService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unitary Tests - FeedbackService")
public class FeedbackServiceTest {

    @Mock
    private FeedbackRepository feedbackRepository;

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
    }
}

package com.redcheck.backend;

import com.redcheck.backend.controller.FeedbackController;
import com.redcheck.backend.dto.request.FeedbackRequestDTO;
import com.redcheck.backend.dto.response.FeedbackResponseDTO;
import com.redcheck.backend.entity.Feedback;
import com.redcheck.backend.security.JwtService;
import com.redcheck.backend.service.FeedbackService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FeedbackController.class)
@DisplayName("Integration Tests - FeedbackController")
public class FeedbackControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private FeedbackService feedbackService;

    @MockitoBean
    private JwtService jwtService;

    private FeedbackRequestDTO feedbackRequestDTO;
    private FeedbackResponseDTO feedbackResponseDTO;

    @BeforeEach
    void setUp() {
        feedbackRequestDTO = FeedbackRequestDTO.builder()
                .category(Feedback.Category.SUGGESTION)
                .message("It would be great to have X")
                .build();

        feedbackResponseDTO = FeedbackResponseDTO.builder()
                .id(1L)
                .category(Feedback.Category.SUGGESTION)
                .message("It would be great to have X")
                .createdDate(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("Endpoint: POST")
    class CreateTests {

        @Test
        @WithMockUser(username = "user@redcheck.com", roles = "USER")
        @DisplayName("With valid data should return created status")
        void create_WithValidData_ShouldReturnCreatedStatus() throws Exception {
            // GIVEN:
            when(feedbackService.createFeedback(any(FeedbackRequestDTO.class), any()))
                    .thenReturn(feedbackResponseDTO);

            String jsonRequest = objectMapper.writeValueAsString(feedbackRequestDTO);

            // WHEN & THEN:
            mockMvc.perform(post("/feedback")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest))

                    .andExpect(status().isCreated())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(1L))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.category").value("SUGGESTION"))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("It would be great to have X"));
        }

        @Test
        @WithMockUser(username = "user@redcheck.com", roles = "USER")
        @DisplayName("Without category should return bad request")
        void create_WithoutCategory_ShouldReturnBadRequest() throws Exception {
            // GIVEN:
            String jsonRequest = "{\"message\": \"missing category\"}";

            // WHEN & THEN:
            mockMvc.perform(post("/feedback")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(username = "user@redcheck.com", roles = "USER")
        @DisplayName("With blank message should return bad request")
        void create_WithBlankMessage_ShouldReturnBadRequest() throws Exception {
            // GIVEN:
            FeedbackRequestDTO invalidRequestDTO = FeedbackRequestDTO.builder()
                    .category(Feedback.Category.OTHER)
                    .message("")
                    .build();

            String jsonRequest = objectMapper.writeValueAsString(invalidRequestDTO);

            // WHEN & THEN:
            mockMvc.perform(post("/feedback")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest))
                    .andExpect(status().isBadRequest());
        }
    }
}

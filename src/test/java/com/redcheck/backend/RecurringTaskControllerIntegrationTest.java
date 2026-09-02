package com.redcheck.backend;

import com.redcheck.backend.controller.RecurringTaskController;
import com.redcheck.backend.dto.request.RecurringTaskRequestDTO;
import com.redcheck.backend.dto.response.RecurringTaskResponseDTO;
import com.redcheck.backend.dto.update.RecurringTaskActiveDTO;
import com.redcheck.backend.security.JwtService;
import com.redcheck.backend.service.RecurringTaskService;
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

import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RecurringTaskController.class)
@DisplayName("Integration Tests - RecurringTaskController")
public class RecurringTaskControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RecurringTaskService recurringTaskService;

    @MockitoBean
    private JwtService jwtService;

    private RecurringTaskRequestDTO requestDTO;
    private RecurringTaskResponseDTO responseDTO;
    private RecurringTaskActiveDTO activeDTO;

    private final Long SUBJECT_ID = 1L;
    private final Long RECURRING_TASK_ID = 100L;
    private final String BASE_URL = "/subjects/" + SUBJECT_ID + "/recurring-tasks";

    @BeforeEach
    void setUp() {
        requestDTO = RecurringTaskRequestDTO.builder()
                .title("Study Math")
                .description("Review chapters")
                .frequency("WEEKLY")
                .subjectId(SUBJECT_ID)
                .build();

        responseDTO = RecurringTaskResponseDTO.builder()
                .id(RECURRING_TASK_ID)
                .title("Study Math")
                .description("Review chapters")
                .frequency("WEEKLY")
                .active(true)
                .subjectId(SUBJECT_ID)
                .build();

        activeDTO = RecurringTaskActiveDTO.builder()
                .active(false)
                .build();
    }

    @Nested
    @DisplayName("Endpoint: GET")
    class GetAllTests {

        @Test
        @WithMockUser(username = "user@redcheck.com", roles = "USER")
        @DisplayName("Should return ok and JSON array")
        void getAll_ShouldReturnOkAndJsonArray() throws Exception {
            // GIVEN
            when(recurringTaskService.getAllRecurringTask(any(), eq(SUBJECT_ID), isNull()))
                    .thenReturn(Collections.singletonList(responseDTO));

            // WHEN & THEN
            mockMvc.perform(get(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON))

                    .andExpect(status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath("$").isArray())
                    .andExpect(MockMvcResultMatchers.jsonPath("$[0].id").value(RECURRING_TASK_ID))
                    .andExpect(MockMvcResultMatchers.jsonPath("$[0].title").value("Study Math"))
                    .andExpect(MockMvcResultMatchers.jsonPath("$[0].active").value(true));
        }
    }

    @Nested
    @DisplayName("Endpoint: POST")
    class CreateTests {

        @Test
        @WithMockUser(username = "user@redcheck.com", roles = "USER")
        @DisplayName("With valid data should return created status")
        void create_WithValidData_ShouldReturnCreatedStatus() throws Exception {
            // GIVEN
            when(recurringTaskService.createRecurringTask(eq(SUBJECT_ID), any(RecurringTaskRequestDTO.class), any()))
                    .thenReturn(responseDTO);

            String jsonRequest = objectMapper.writeValueAsString(requestDTO);

            // WHEN & THEN
            mockMvc.perform(post(BASE_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest))

                    .andExpect(status().isCreated())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(RECURRING_TASK_ID))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.title").value("Study Math"));
        }

        @Test
        @WithMockUser(username = "user@redcheck.com", roles = "USER")
        @DisplayName("With invalid data should return bad request")
        void create_WithInvalidData_ShouldReturnBadRequest() throws Exception {
            // GIVEN:
            RecurringTaskRequestDTO invalidRequestDTO = RecurringTaskRequestDTO.builder()
                    .title(null)
                    .description("Review chapters")
                    .frequency("WEEKLY")
                    .subjectId(SUBJECT_ID)
                    .build();

            String jsonRequest = objectMapper.writeValueAsString(invalidRequestDTO);

            // WHEN & THEN
            mockMvc.perform(post(BASE_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest))

                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Endpoint: PUT")
    class UpdateTests {

        @Test
        @WithMockUser(username = "user@redcheck.com", roles = "USER")
        @DisplayName("With valid data should return ok status")
        void update_WithValidData_ShouldReturnOkStatus() throws Exception {
            // GIVEN
            when(recurringTaskService.updateRecurringTask(eq(SUBJECT_ID), eq(RECURRING_TASK_ID), any(RecurringTaskRequestDTO.class), any()))
                    .thenReturn(responseDTO);

            String jsonRequest = objectMapper.writeValueAsString(requestDTO);

            // WHEN & THEN
            mockMvc.perform(put(BASE_URL + "/" + RECURRING_TASK_ID)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest))

                    .andExpect(status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(RECURRING_TASK_ID));
        }
    }

    @Nested
    @DisplayName("Endpoint: DELETE")
    class DeleteTests {

        @Test
        @WithMockUser(username = "user@redcheck.com", roles = "USER")
        @DisplayName("When deleted should return no content status")
        void delete_ShouldReturnNoContent() throws Exception {
            // GIVEN:

            // WHEN & THEN
            mockMvc.perform(delete(BASE_URL + "/" + RECURRING_TASK_ID)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON))

                    .andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("Endpoint: PATCH")
    class ActiveTests {

        @Test
        @WithMockUser(username = "user@redcheck.com", roles = "USER")
        @DisplayName("When valid should update active status and return ok")
        void active_WhenValid_ShouldReturnOk() throws Exception {
            // GIVEN
            RecurringTaskResponseDTO inactiveResponseDTO = RecurringTaskResponseDTO.builder()
                    .id(RECURRING_TASK_ID)
                    .title("Study Math")
                    .description("Review chapters")
                    .frequency("WEEKLY")
                    .active(false)
                    .subjectId(SUBJECT_ID)
                    .build();

            when(recurringTaskService.activateRecurringTask(eq(SUBJECT_ID), eq(RECURRING_TASK_ID), any(RecurringTaskActiveDTO.class), any()))
                    .thenReturn(inactiveResponseDTO);

            String jsonRequest = objectMapper.writeValueAsString(activeDTO);

            // WHEN & THEN
            mockMvc.perform(patch(BASE_URL + "/" + RECURRING_TASK_ID + "/active")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest))

                    .andExpect(status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.active").value(false));
        }
    }
}
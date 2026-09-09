package com.redcheck.backend;

import com.redcheck.backend.controller.TaskCalendarController;
import com.redcheck.backend.dto.response.TaskResponseDTO;
import com.redcheck.backend.security.JwtService;
import com.redcheck.backend.service.TaskService;
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

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TaskCalendarController.class)
@DisplayName("Integration Tests - TaskCalendarController")
public class TaskCalendarControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TaskService taskService;

    @MockitoBean
    private JwtService jwtService;

    private TaskResponseDTO taskResponseDTO;

    @BeforeEach
    void setUp() {
        taskResponseDTO = TaskResponseDTO.builder()
                .id(1L)
                .title("task")
                .subjectId(10L)
                .completed(true)
                .build();
    }

    @Nested
    @DisplayName("Endpoint: GET /tasks")
    class GetByDateRangeTests {

        @Test
        @WithMockUser(username = "user@redcheck.com", roles = "USER")
        @DisplayName("With a valid range should return ok and JSON array")
        void getByDateRange_WithValidRange_ShouldReturnOkAndJsonArray() throws Exception {
            // GIVEN:
            when(taskService.getTasksForDateRange(any(), any(), any()))
                    .thenReturn(Collections.singletonList(taskResponseDTO));

            // WHEN & THEN:
            mockMvc.perform(get("/tasks?from=2026-01-01&to=2026-01-07")
                            .contentType(MediaType.APPLICATION_JSON))

                    .andExpect(status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath("$").isArray())
                    .andExpect(MockMvcResultMatchers.jsonPath("$[0].id").value(1L))
                    .andExpect(MockMvcResultMatchers.jsonPath("$[0].completed").value(true));
        }

        @Test
        @WithMockUser(username = "user@redcheck.com", roles = "USER")
        @DisplayName("Without required params should return bad request")
        void getByDateRange_WithoutParams_ShouldReturnBadRequest() throws Exception {
            // WHEN & THEN:
            mockMvc.perform(get("/tasks")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }
    }
}

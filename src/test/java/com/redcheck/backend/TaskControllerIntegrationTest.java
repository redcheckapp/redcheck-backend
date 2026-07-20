package com.redcheck.backend;

import com.redcheck.backend.controller.TaskController;
import com.redcheck.backend.dto.request.TaskRequestDTO;
import com.redcheck.backend.dto.response.TaskResponseDTO;
import com.redcheck.backend.dto.update.TaskCompleteDTO;
import com.redcheck.backend.security.JwtService;
import com.redcheck.backend.service.TaskService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import tools.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@WebMvcTest(TaskController.class)
@DisplayName("Integration Tests - TaskController")
public class TaskControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TaskService taskService;

    @MockitoBean
    private JwtService jwtService;

    private TaskRequestDTO taskRequestDTO;
    private TaskResponseDTO taskResponseDTO;
    private TaskResponseDTO taskCompletedResponseDTO;
    private TaskCompleteDTO taskCompleteDTO;

    @BeforeEach
    void setUp() {
        taskRequestDTO = TaskRequestDTO.builder()
                .title("New task")
                .description("A new example task")
                .deadline(LocalDateTime.now().plusDays(3))
                .build();

        taskResponseDTO = TaskResponseDTO.builder()
                .id(1L)
                .title("New task")
                .assignedDate(LocalDateTime.now())
                .deadline(LocalDateTime.now().plusDays(3))
                .completedDate(null)
                .completed(false)
                .overdue(false)
                .subjectId(10L)
                .build();

        taskCompletedResponseDTO = TaskResponseDTO.builder()
                .id(1L)
                .title("New task")
                .assignedDate(LocalDateTime.now())
                .deadline(LocalDateTime.now().plusDays(3))
                .completedDate(LocalDateTime.now())
                .completed(true)
                .overdue(false)
                .subjectId(10L)
                .build();

        taskCompleteDTO = TaskCompleteDTO.builder()
                .completed(true)
                .build();
    }

    @Nested
    @DisplayName("Endpoint: GET")
    class GetAllTasksTests {

        @Test
        @WithMockUser(username = "user@redcheck.com", roles = "USER")
        @DisplayName("Should return ok and JSON array")
        void getAllTasks_ShouldReturnOkAndJsonArray() throws Exception {
            // GIVEN:
            when(taskService.getAllTask(any(), eq(10L), isNull(), isNull(), isNull()))
                    .thenReturn(Collections.singletonList(taskResponseDTO));

            // WHEN & THEN:
            mockMvc.perform(get("/subjects/10/tasks")
                    .contentType(MediaType.APPLICATION_JSON))

                    .andExpect(status().isOk())
                    .andExpect(MockMvcResultMatchers.content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))

                    .andExpect(MockMvcResultMatchers.jsonPath("$").isArray())
                    .andExpect(MockMvcResultMatchers.jsonPath("$[0].id").value(1L))
                    .andExpect(MockMvcResultMatchers.jsonPath("$[0].title").value("New task"));
        }
    }

    @Nested
    @DisplayName("Endpoint: POST")
    class CreateTaskTest {

        @Test
        @WithMockUser(username = "user@redcheck.com", roles = "USER")
        @DisplayName("With valid data should return created status")
        void createTask_WithValidData_ShouldReturnCreatedStatus() throws Exception {
            // GIVEN:
            when(taskService.createTask(eq(10L), any(TaskRequestDTO.class), any()))
                    .thenReturn(taskResponseDTO);

            String jsonRequest = objectMapper.writeValueAsString(taskRequestDTO);

            // WHEN & THEN:
            mockMvc.perform(post("/subjects/10/tasks")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest))

                            .andExpect(status().isCreated())
                            .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(1L))
                            .andExpect(MockMvcResultMatchers.jsonPath("$.title").value("New task"));
        }

        @Test
        @WithMockUser(username = "user@redcheck.com", roles = "USER")
        @DisplayName("With invalid data should return bad request")
        void createTask_WithInvalidData_ShouldReturnBadRequest() throws Exception {
            // GIVEN:
            when(taskService.createTask(eq(10L), any(TaskRequestDTO.class), any()))
                    .thenReturn(taskResponseDTO);

            taskRequestDTO.setTitle(null);
            String jsonRequest = objectMapper.writeValueAsString(taskRequestDTO);

            // WHEN & THEN:
            mockMvc.perform(post("/subjects/10/tasks")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest))

                            .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Endpoint: PUT")
    class UpdateTaskTests {

        @Test
        @WithMockUser(username = "user@redcheck.com", roles = "USER")
        @DisplayName("With valid data should return ok status")
        void updateTask_WithValidData_ShouldReturnOkStatus() throws Exception {
            // GIVEN:
            when(taskService.updateTask(eq(10L), any(), any(TaskRequestDTO.class), any()))
                    .thenReturn(taskResponseDTO);

            String jsonRequest = objectMapper.writeValueAsString(taskRequestDTO);

            // WHEN & THEN:
            mockMvc.perform(put("/subjects/10/tasks/1")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest))

                            .andExpect(status().isOk())
                            .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(1L))
                            .andExpect(MockMvcResultMatchers.jsonPath("$.title").value("New task"));
        }
    }

    @Nested
    @DisplayName("Endpoint: DELETE")
    class DeleteTaskTests {

        @Test
        @WithMockUser(username = "user@redcheck.com", roles = "USER")
        @DisplayName("When task exists should return success")
        void deleteTask_WhenTaskExists_ShouldReturnSuccess() throws Exception {
            // GIVEN: deleteTask is void, so we dont need when()

            // WHEN & THEN:
            mockMvc.perform(delete("/subjects/10/tasks/1")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON))
                            .andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("Endpoint: GET /trash")
    class GetTrashTasksTests {

        @Test
        @WithMockUser(username = "user@redcheck.com", roles = "USER")
        @DisplayName("Should return ok and JSON array of deleted tasks")
        void getTrashTasks_ShouldReturnOkAndJsonArray() throws Exception {
            // GIVEN:
            when(taskService.getAllTask(any(), eq(10L), isNull(), isNull(), eq(true)))
                    .thenReturn(Collections.singletonList(taskResponseDTO));

            // WHEN & THEN:
            mockMvc.perform(get("/subjects/10/tasks?deleted=true")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath("$").isArray())
                    .andExpect(MockMvcResultMatchers.jsonPath("$[0].id").value(1L));
        }
    }

    @Nested
    @DisplayName("Endpoint: PATCH /{taskId}/restore")
    class RestoreTaskTests {

        @Test
        @WithMockUser(username = "user@redcheck.com", roles = "USER")
        @DisplayName("Restore task should return ok status")
        void restoreTask_ShouldReturnOkStatus() throws Exception {
            // GIVEN:
            when(taskService.restoreTask(eq(10L), eq(1L), any()))
                    .thenReturn(taskResponseDTO);

            // WHEN & THEN:
            mockMvc.perform(patch("/subjects/10/tasks/1/restore")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(1L));
        }
    }

    @Nested
    @DisplayName("Endpoint: DELETE /{taskId}/force")
    class HardDeleteTaskTests {

        @Test
        @WithMockUser(username = "user@redcheck.com", roles = "USER")
        @DisplayName("When hard deleting task should return success")
        void hardDeleteTask_ShouldReturnSuccess() throws Exception {
            // WHEN & THEN:
            mockMvc.perform(delete("/subjects/10/tasks/1/force")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("Endpoint: PATCH")
    class MarkTaskAsCompletedTests {

        @Test
        @WithMockUser(username = "user@redcheck.com", roles = "USER")
        @DisplayName("Mark as completed should return ok status")
        void markTaskAsCompleted_ShouldReturnOkStatus() throws Exception {
            // GIVEN:
            when(taskService.markTaskAsCompleted(eq(10L), any(), any(TaskCompleteDTO.class), any()))
                    .thenReturn(taskCompletedResponseDTO);

            String jsonRequest = objectMapper.writeValueAsString(taskCompleteDTO);

            // WHEN & THEN:
            mockMvc.perform(patch("/subjects/10/tasks/1/complete")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest))

                            .andExpect(status().isOk())
                            .andExpect(MockMvcResultMatchers.jsonPath("$.completed").value(true));
        }
    }
}

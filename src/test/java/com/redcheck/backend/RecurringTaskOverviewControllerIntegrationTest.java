package com.redcheck.backend;

import com.redcheck.backend.controller.RecurringTaskOverviewController;
import com.redcheck.backend.dto.response.RecurringTaskResponseDTO;
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

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RecurringTaskOverviewController.class)
@DisplayName("Integration Tests - RecurringTaskOverviewController")
public class RecurringTaskOverviewControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RecurringTaskService recurringTaskService;

    @MockitoBean
    private JwtService jwtService;

    private RecurringTaskResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        responseDTO = RecurringTaskResponseDTO.builder()
                .id(1L)
                .title("Study Math")
                .frequency("WEEKLY")
                .active(true)
                .subjectId(10L)
                .build();
    }

    @Nested
    @DisplayName("Endpoint: GET /recurring-tasks")
    class GetAllTests {

        @Test
        @WithMockUser(username = "user@redcheck.com", roles = "USER")
        @DisplayName("Without active param should return every routine across all subjects")
        void getAll_WithoutActiveParam_ShouldReturnEveryRoutine() throws Exception {
            // GIVEN:
            when(recurringTaskService.getAllRecurringTask(any(), isNull(), isNull()))
                    .thenReturn(Collections.singletonList(responseDTO));

            // WHEN & THEN:
            mockMvc.perform(get("/recurring-tasks")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath("$").isArray())
                    .andExpect(MockMvcResultMatchers.jsonPath("$[0].id").value(1L))
                    .andExpect(MockMvcResultMatchers.jsonPath("$[0].subjectId").value(10L));
        }

        @Test
        @WithMockUser(username = "user@redcheck.com", roles = "USER")
        @DisplayName("With active=true should pass it through to the service")
        void getAll_WithActiveParam_ShouldPassItThrough() throws Exception {
            // GIVEN:
            when(recurringTaskService.getAllRecurringTask(any(), isNull(), eq(true)))
                    .thenReturn(Collections.singletonList(responseDTO));

            // WHEN & THEN:
            mockMvc.perform(get("/recurring-tasks?active=true")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath("$[0].active").value(true));
        }
    }
}

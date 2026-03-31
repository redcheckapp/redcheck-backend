package com.redcheck.backend;

import com.redcheck.backend.controller.SubjectController;
import com.redcheck.backend.dto.request.SubjectRequestDTO;
import com.redcheck.backend.dto.response.SubjectResponseDTO;
import com.redcheck.backend.dto.update.SubjectArchiveDTO;
import com.redcheck.backend.security.JwtService;
import com.redcheck.backend.service.SubjectService;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@WebMvcTest(SubjectController.class)
@DisplayName("Integration Tests - SubjectController")
public class SubjectControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SubjectService subjectService;

    @MockitoBean
    private JwtService jwtService;

    private SubjectRequestDTO subjectRequestDTO;
    private SubjectResponseDTO subjectResponseDTO;
    private SubjectArchiveDTO subjectArchiveDTO;

    @BeforeEach
    void setUp() {
        subjectRequestDTO = SubjectRequestDTO.builder()
                .name("subject")
                .description("description")
                .build();

        subjectResponseDTO = SubjectResponseDTO.builder()
                .id(1L)
                .name("new subject")
                .description("new description")
                .archived(true)
                .build();

        subjectArchiveDTO = SubjectArchiveDTO.builder()
                .archived(true)
                .build();
    }

    @Nested
    @DisplayName("Endpoint: GET")
    class GetAllTests {

        @Test
        @WithMockUser(username = "user@redcheck.com", roles = "USER")
        @DisplayName("Should return ok and JSON array")
        void getAll_ShouldReturnOkAndJsonArray() throws Exception {
            // GIVEN:
            when(subjectService.getAllSubjects(any(), isNull()))
                    .thenReturn(Collections.singletonList(subjectResponseDTO));

            // WHEN & THEN:
            mockMvc.perform(get("/subjects")
                    .contentType(MediaType.APPLICATION_JSON))

                    .andExpect(status().isOk())
                    .andExpect(MockMvcResultMatchers.content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))

                    .andExpect(MockMvcResultMatchers.jsonPath("$").isArray())
                    .andExpect(MockMvcResultMatchers.jsonPath("$[0].id").value(1L))
                    .andExpect(MockMvcResultMatchers.jsonPath("$[0].name").value("new subject"))
                    .andExpect(MockMvcResultMatchers.jsonPath("$[0].description").value("new description"));
        }
    }

    @Nested
    @DisplayName("Endpoint: POST")
    class CreateTests {

        @Test
        @WithMockUser(username = "user@redcheck.com", roles = "USER")
        @DisplayName("With valid data should return created status")
        void create_WithValidData_ShouldReturnCreatedStatus() throws Exception {
            // GIVEN:
            when(subjectService.createSubject(any(SubjectRequestDTO.class), any()))
                    .thenReturn(subjectResponseDTO);

            String jsonRequest = objectMapper.writeValueAsString(subjectRequestDTO);

            // WHEN & THEN:
            mockMvc.perform(post("/subjects")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest))

                    .andExpect(status().isCreated())

                    .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(1L))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.name").value("new subject"))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("new description"));
        }

        @Test
        @WithMockUser(username = "user@redcheck.com", roles = "USER")
        @DisplayName("With invalid data should return bad request")
        void createTask_WithInvalidData_ShouldReturnBadRequest() throws Exception {
            // GIVEN:
            when(subjectService.createSubject(any(SubjectRequestDTO.class), any()))
                    .thenReturn(subjectResponseDTO);

            subjectRequestDTO.setName(null);
            String jsonRequest = objectMapper.writeValueAsString(subjectRequestDTO);

            // WHEN & THEN:
            mockMvc.perform(post("/subjects")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest))

                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Endpoint: PUT")
    class ModifyTests {

        @Test
        @WithMockUser(username = "user@redcheck.com", roles = "USER")
        @DisplayName("With valid data should return ok status")
        void modify_WithValidData_ShouldReturnOkStatus() throws Exception {
            // GIVEN:
            when(subjectService.modifySubject(any(), any(SubjectRequestDTO.class), any()))
                    .thenReturn(subjectResponseDTO);

            String jsonRequest = objectMapper.writeValueAsString(subjectRequestDTO);

            // WHEN & THEN:
            mockMvc.perform(put("/subjects/1")
                            .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonRequest))

                    .andExpect(status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(1L))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.name").value("new subject"))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("new description"));
        }
    }

    @Nested
    @DisplayName("Endpoint: DELETE")
    class DeleteTests {

        @Test
        @WithMockUser(username = "user@redcheck.com", roles = "USER")
        @DisplayName("When subject exists should return success")
        void delete_WhenTaskExists_ShouldReturnSuccess() throws Exception {
            // GIVEN:

            // WHEN & THEN:
            mockMvc.perform(delete("/subjects/1")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("Endpoint: PATCH")
    class ArchiveTests {

        @Test
        @WithMockUser(username = "user@redcheck.com", roles = "USER")
        @DisplayName("Archive subject should return ok status")
        void archive_ShouldReturnOkStatus() throws Exception {
            // GIVEN:
            when(subjectService.archiveSubject(eq(1L), any(SubjectArchiveDTO.class), any()))
                    .thenReturn(subjectResponseDTO);

            String jsonRequest = objectMapper.writeValueAsString(subjectArchiveDTO);

            // WHEN & THEN:
            mockMvc.perform(patch("/subjects/1/archive")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest))

                    .andExpect(status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.archived").value(true));
        }
    }
}

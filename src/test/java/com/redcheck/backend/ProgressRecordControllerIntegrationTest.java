package com.redcheck.backend;

import com.redcheck.backend.controller.ProgressRecordController;
import com.redcheck.backend.dto.response.ProgressRecordResponseDTO;
import com.redcheck.backend.entity.User;
import com.redcheck.backend.security.JwtService;
import com.redcheck.backend.service.ProgressRecordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;

import java.time.LocalDate;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProgressRecordController.class)
@DisplayName("Integration Tests - ProgressRecordController")
public class ProgressRecordControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProgressRecordService progressRecordService;

    @MockitoBean
    private JwtService jwtService;

    private User mockUser;
    private UsernamePasswordAuthenticationToken mockAuthToken;
    private ProgressRecordResponseDTO progressRecordResponseDTO;
    private LocalDate testDate;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .username("user")
                .email("user@redcheck.com")
                .password("redcheckUser")
                .build();
        mockUser.setId(1L);

        mockAuthToken = new UsernamePasswordAuthenticationToken(
                mockUser,
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );

        testDate = LocalDate.of(2023, 10, 15);

        progressRecordResponseDTO = ProgressRecordResponseDTO.builder()
                .date(testDate)
                .totalTasks(10)
                .completedTasks(5)
                .completionRate(0.5)
                .build();
    }

    @Nested
    @DisplayName("Endpoint: GET")
    class GetHeatmapTests {

        @Test
        @DisplayName("When authenticated should return ok and JSON array of records")
        void getHeatmap_ShouldReturnOkAndJsonArray() throws Exception {
            when(progressRecordService.getHeatmap(any(User.class)))
                    .thenReturn(Collections.singletonList(progressRecordResponseDTO));

            mockMvc.perform(get("/progress/heatmap")
                            .with(authentication(mockAuthToken))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(MockMvcResultMatchers.content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(MockMvcResultMatchers.jsonPath("$").isArray())
                    .andExpect(MockMvcResultMatchers.jsonPath("$[0].totalTasks").value(10))
                    .andExpect(MockMvcResultMatchers.jsonPath("$[0].completedTasks").value(5))
                    .andExpect(MockMvcResultMatchers.jsonPath("$[0].completionRate").value(0.5))
                    .andExpect(MockMvcResultMatchers.jsonPath("$[0].date").value(testDate.toString()));
        }
    }

    @Nested
    @DisplayName("Endpoint: GET")
    class GetByDayTests {

        @Test
        @DisplayName("When authenticated and valid date should return ok and record DTO")
        void getByDay_ShouldReturnOkAndRecordDTO() throws Exception {
            when(progressRecordService.getDayProgress(any(User.class), eq(testDate)))
                    .thenReturn(progressRecordResponseDTO);

            mockMvc.perform(get("/progress/day")
                            .param("date", testDate.toString())
                            .with(authentication(mockAuthToken))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(MockMvcResultMatchers.content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.totalTasks").value(10))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.completedTasks").value(5))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.completionRate").value(0.5))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.date").value(testDate.toString()));
        }

        @Test
        @DisplayName("When date param is missing should return bad request")
        void getByDay_WhenDateMissing_ShouldReturnBadRequest() throws Exception {
            mockMvc.perform(get("/progress/day")
                            .with(authentication(mockAuthToken))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }
    }
}
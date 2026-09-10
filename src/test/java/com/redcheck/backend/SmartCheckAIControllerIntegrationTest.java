package com.redcheck.backend;

import com.redcheck.backend.controller.SmartCheckAIController;
import com.redcheck.backend.entity.User;
import com.redcheck.backend.security.JwtService;
import com.redcheck.backend.service.SmartCheckAIService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SmartCheckAIController.class)
@DisplayName("Integration Tests - SmartCheckAIController")
public class SmartCheckAIControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SmartCheckAIService smartCheckAIService;

    @MockitoBean
    private JwtService jwtService;

    private User mockUser;
    private UsernamePasswordAuthenticationToken mockAuthToken;

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
    }

    @Nested
    @DisplayName("Endpoint: POST /ai/analyze")
    class DailySmartAnalysisTests {

        @Test
        @DisplayName("When the user has no pending tasks should return bad request and never start the analysis")
        void dailySmartAnalysis_WhenNoPendingTasks_ShouldReturnBadRequestAndNeverRun() throws Exception {
            when(smartCheckAIService.hasPendingTasks(any(User.class))).thenReturn(false);

            mockMvc.perform(post("/ai/analyze")
                            .with(authentication(mockAuthToken))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());

            verify(smartCheckAIService, never()).deleteTodaysAnalysis(any(User.class));
            verify(smartCheckAIService, never()).runDailySmartAnalysis(any(User.class), anyString());
        }

        @Test
        @DisplayName("When the user has pending tasks should return ok and start the analysis")
        void dailySmartAnalysis_WhenHasPendingTasks_ShouldReturnOkAndRunAnalysis() throws Exception {
            when(smartCheckAIService.hasPendingTasks(any(User.class))).thenReturn(true);

            mockMvc.perform(post("/ai/analyze")
                            .with(authentication(mockAuthToken))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            verify(smartCheckAIService, times(1)).deleteTodaysAnalysis(any(User.class));
            verify(smartCheckAIService, times(1)).runDailySmartAnalysis(any(User.class), anyString());
        }
    }
}

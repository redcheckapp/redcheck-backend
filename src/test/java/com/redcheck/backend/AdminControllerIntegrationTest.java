package com.redcheck.backend;

import com.redcheck.backend.controller.AdminController;
import com.redcheck.backend.dto.response.AdminFeedbackResponseDTO;
import com.redcheck.backend.dto.response.AiUsageStatsResponseDTO;
import com.redcheck.backend.dto.update.AdminFeedbackUpdateDTO;
import com.redcheck.backend.entity.Feedback;
import com.redcheck.backend.entity.User;
import com.redcheck.backend.exception.FeedbackAuthorDeletedException;
import com.redcheck.backend.exception.FeedbackNotFoundException;
import com.redcheck.backend.dto.request.FeedbackReplyRequestDTO;
import com.redcheck.backend.security.JwtService;
import com.redcheck.backend.security.SecurityConfig;
import com.redcheck.backend.service.AdminAccessService;
import com.redcheck.backend.service.AiUsageService;
import com.redcheck.backend.service.FeedbackService;
import com.redcheck.backend.service.SystemStatusService;
import com.redcheck.backend.service.UserStatsService;
import com.redcheck.backend.dto.response.SystemStatusResponseDTO;
import com.redcheck.backend.dto.response.UserStatsResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Imports the real SecurityConfig (unlike the other controller tests) since
// the /admin/** access rule lives there and is the whole point of this API.
@WebMvcTest(AdminController.class)
@Import(SecurityConfig.class)
@DisplayName("Integration Tests - AdminController")
public class AdminControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private FeedbackService feedbackService;

    @MockitoBean
    private AiUsageService aiUsageService;

    @MockitoBean
    private AdminAccessService adminAccessService;

    @MockitoBean
    private UserStatsService userStatsService;

    @MockitoBean
    private SystemStatusService systemStatusService;

    @MockitoBean
    private AuthenticationProvider authenticationProvider;

    @MockitoBean
    private JwtService jwtService;

    private User adminUser;
    private UsernamePasswordAuthenticationToken adminAuth;
    private UsernamePasswordAuthenticationToken userAuth;

    private static UsernamePasswordAuthenticationToken authFor(User user) {
        return new UsernamePasswordAuthenticationToken(
                user, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @BeforeEach
    void setUp() {
        adminUser = User.builder().username("dev").email("dev@redcheck.com").build();
        adminUser.setId(1L);
        User regularUser = User.builder().username("user").email("user@redcheck.com").build();
        regularUser.setId(2L);

        adminAuth = authFor(adminUser);
        userAuth = authFor(regularUser);

        // Matched by email, not by the User instance: User's equals() has no
        // explicitly-included fields, so every User compares equal.
        when(adminAccessService.isAdmin(argThat(user -> user != null
                && "dev@redcheck.com".equals(user.getEmail())))).thenReturn(true);
    }

    @Nested
    @DisplayName("Access control")
    class AccessTests {

        @Test
        @DisplayName("A non-admin user should be forbidden")
        void adminEndpoints_ForNonAdmin_ShouldReturnForbidden() throws Exception {
            mockMvc.perform(get("/admin/feedback").with(authentication(userAuth)))
                    .andExpect(status().isForbidden());

            verify(feedbackService, never()).getAllFeedbackForAdmin();
        }

        @Test
        @DisplayName("An anonymous request should be rejected")
        void adminEndpoints_WhenAnonymous_ShouldBeRejected() throws Exception {
            mockMvc.perform(get("/admin/ai-usage"))
                    .andExpect(status().isForbidden());

            verify(aiUsageService, never()).getStats(anyInt());
        }
    }

    @Nested
    @DisplayName("Endpoint: GET /admin/feedback")
    class GetFeedbackTests {

        @Test
        @DisplayName("For an admin should return every feedback entry")
        void getAllFeedback_ForAdmin_ShouldReturnList() throws Exception {
            // GIVEN:
            when(feedbackService.getAllFeedbackForAdmin()).thenReturn(List.of(
                    AdminFeedbackResponseDTO.builder()
                            .id(1L)
                            .category(Feedback.Category.BUG)
                            .status(Feedback.Status.NEW)
                            .message("Broken")
                            .createdDate(LocalDateTime.now())
                            .userEmail("user@redcheck.com")
                            .build()));

            // WHEN & THEN:
            mockMvc.perform(get("/admin/feedback").with(authentication(adminAuth)))
                    .andExpect(status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath("$[0].category").value("BUG"))
                    .andExpect(MockMvcResultMatchers.jsonPath("$[0].status").value("NEW"))
                    .andExpect(MockMvcResultMatchers.jsonPath("$[0].userEmail").value("user@redcheck.com"));
        }
    }

    @Nested
    @DisplayName("Endpoint: PATCH /admin/feedback/{id}")
    class UpdateFeedbackTests {

        @Test
        @DisplayName("With valid data should return the updated entry")
        void updateFeedback_WithValidData_ShouldReturnOk() throws Exception {
            // GIVEN:
            AdminFeedbackUpdateDTO update = AdminFeedbackUpdateDTO.builder().status(Feedback.Status.DONE).build();
            when(feedbackService.updateFeedbackForAdmin(eq(1L), any(AdminFeedbackUpdateDTO.class)))
                    .thenReturn(AdminFeedbackResponseDTO.builder().id(1L).status(Feedback.Status.DONE).build());

            // WHEN & THEN:
            mockMvc.perform(patch("/admin/feedback/1")
                            .with(authentication(adminAuth))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(update)))
                    .andExpect(status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("DONE"));
        }

        @Test
        @DisplayName("For a missing entry should return not found")
        void updateFeedback_WhenNotFound_ShouldReturnNotFound() throws Exception {
            // GIVEN:
            when(feedbackService.updateFeedbackForAdmin(eq(99L), any(AdminFeedbackUpdateDTO.class)))
                    .thenThrow(new FeedbackNotFoundException(99L));

            // WHEN & THEN:
            mockMvc.perform(patch("/admin/feedback/99")
                            .with(authentication(adminAuth))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"SEEN\"}"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Endpoint: GET /admin/ai-usage")
    class AiUsageTests {

        @Test
        @DisplayName("Should clamp the requested window to 90 days")
        void getAiUsage_ShouldClampDays() throws Exception {
            // GIVEN:
            when(aiUsageService.getStats(90)).thenReturn(AiUsageStatsResponseDTO.builder()
                    .days(90).daily(List.of()).build());

            // WHEN & THEN:
            mockMvc.perform(get("/admin/ai-usage").param("days", "500").with(authentication(adminAuth)))
                    .andExpect(status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.days").value(90));

            verify(aiUsageService).getStats(90);
        }
    }

    @Nested
    @DisplayName("Endpoint: POST /admin/feedback/{id}/notify")
    class NotifyAuthorTests {

        @Test
        @DisplayName("With a message should return the updated entry")
        void notifyAuthor_WithMessage_ShouldReturnOk() throws Exception {
            when(feedbackService.notifyAuthor(eq(1L), any(FeedbackReplyRequestDTO.class)))
                    .thenReturn(AdminFeedbackResponseDTO.builder().id(1L).userNotifiedDate(LocalDateTime.now()).build());

            mockMvc.perform(post("/admin/feedback/1/notify")
                            .with(authentication(adminAuth))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"message\":\"It's live!\"}"))
                    .andExpect(status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.userNotifiedDate").exists());
        }

        @Test
        @DisplayName("With a blank message should return bad request")
        void notifyAuthor_WithBlankMessage_ShouldReturnBadRequest() throws Exception {
            mockMvc.perform(post("/admin/feedback/1/notify")
                            .with(authentication(adminAuth))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"message\":\"  \"}"))
                    .andExpect(status().isBadRequest());

            verify(feedbackService, never()).notifyAuthor(any(), any());
        }

        @Test
        @DisplayName("When the author deleted their account should return conflict")
        void notifyAuthor_WhenAuthorDeleted_ShouldReturnConflict() throws Exception {
            when(feedbackService.notifyAuthor(eq(2L), any(FeedbackReplyRequestDTO.class)))
                    .thenThrow(new FeedbackAuthorDeletedException(2L));

            mockMvc.perform(post("/admin/feedback/2/notify")
                            .with(authentication(adminAuth))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"message\":\"Thanks\"}"))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("A non-admin user should be forbidden")
        void notifyAuthor_ForNonAdmin_ShouldReturnForbidden() throws Exception {
            mockMvc.perform(post("/admin/feedback/1/notify")
                            .with(authentication(userAuth))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"message\":\"Thanks\"}"))
                    .andExpect(status().isForbidden());

            verify(feedbackService, never()).notifyAuthor(any(), any());
        }
    }

    @Nested
    @DisplayName("Endpoints: GET /admin/user-stats and /admin/system")
    class StatsAndSystemTests {

        @Test
        @DisplayName("User stats should clamp the window like AI usage")
        void getUserStats_ShouldClampDays() throws Exception {
            when(userStatsService.getStats(1)).thenReturn(UserStatsResponseDTO.builder().days(1).totalUsers(12).daily(List.of()).build());

            mockMvc.perform(get("/admin/user-stats").param("days", "0").with(authentication(adminAuth)))
                    .andExpect(status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.totalUsers").value(12));

            verify(userStatsService).getStats(1);
        }

        @Test
        @DisplayName("System status should return the service's snapshot")
        void getSystemStatus_ShouldReturnOk() throws Exception {
            when(systemStatusService.getStatus()).thenReturn(SystemStatusResponseDTO.builder()
                    .gitSha("abc1234").aiEngineReachable(true).aiEngineLatencyMs(42L).build());

            mockMvc.perform(get("/admin/system").with(authentication(adminAuth)))
                    .andExpect(status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.gitSha").value("abc1234"))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.aiEngineReachable").value(true));
        }

        @Test
        @DisplayName("A non-admin user should be forbidden from both")
        void statsEndpoints_ForNonAdmin_ShouldReturnForbidden() throws Exception {
            mockMvc.perform(get("/admin/user-stats").with(authentication(userAuth))).andExpect(status().isForbidden());
            mockMvc.perform(get("/admin/system").with(authentication(userAuth))).andExpect(status().isForbidden());
        }
    }
}

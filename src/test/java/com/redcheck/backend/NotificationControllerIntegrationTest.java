package com.redcheck.backend;

import com.redcheck.backend.controller.NotificationController;
import com.redcheck.backend.dto.response.NotificationResponseDTO;
import com.redcheck.backend.entity.Notification;
import com.redcheck.backend.entity.User;
import com.redcheck.backend.exception.NotificationNotFoundException;
import com.redcheck.backend.security.JwtService;
import com.redcheck.backend.service.NotificationService;
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

import java.time.LocalDateTime;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
@DisplayName("Integration Tests - NotificationController")
public class NotificationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private JwtService jwtService;

    private User mockUser;
    private UsernamePasswordAuthenticationToken mockAuthToken;
    private NotificationResponseDTO notificationResponseDTO;

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

        notificationResponseDTO = NotificationResponseDTO.builder()
                .id(1L)
                .title("Test Notification")
                .message("This is a test message")
                .read(false)
                .creationDate(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("Endpoint: GET")
    class GetNotificationsTests {

        @Test
        @DisplayName("When no 'read' param is provided should return ok and all notifications")
        void getNotifications_WhenNoReadParamProvided_ShouldReturnAllNotifications() throws Exception {
            when(notificationService.getNotifications(any(User.class), isNull()))
                    .thenReturn(Collections.singletonList(notificationResponseDTO));

            mockMvc.perform(get("/notifications")
                            .with(authentication(mockAuthToken))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(MockMvcResultMatchers.content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(MockMvcResultMatchers.jsonPath("$").isArray())
                    .andExpect(MockMvcResultMatchers.jsonPath("$[0].id").value(1L))
                    .andExpect(MockMvcResultMatchers.jsonPath("$[0].title").value("Test Notification"))
                    .andExpect(MockMvcResultMatchers.jsonPath("$[0].message").value("This is a test message"))
                    .andExpect(MockMvcResultMatchers.jsonPath("$[0].read").value(false));
        }

        @Test
        @DisplayName("When 'read' param is provided should return ok and filtered notifications")
        void getNotifications_WhenReadParamProvided_ShouldReturnFilteredNotifications() throws Exception {
            Boolean readParam = false;
            when(notificationService.getNotifications(any(User.class), eq(readParam)))
                    .thenReturn(Collections.singletonList(notificationResponseDTO));

            mockMvc.perform(get("/notifications")
                            .param("read", readParam.toString())
                            .with(authentication(mockAuthToken))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(MockMvcResultMatchers.content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(MockMvcResultMatchers.jsonPath("$").isArray())
                    .andExpect(MockMvcResultMatchers.jsonPath("$[0].id").value(1L))
                    .andExpect(MockMvcResultMatchers.jsonPath("$[0].read").value(false));
        }
    }

    @Nested
    @DisplayName("Endpoint: GET with type")
    class GetByTypeTests {

        @Test
        @DisplayName("With a type param should delegate to the type-filtered lookup")
        void getNotifications_WithTypeParam_ShouldFilterByType() throws Exception {
            when(notificationService.getNotificationsByType(any(User.class), eq(false), eq(Notification.Type.FEEDBACK_REPLY)))
                    .thenReturn(Collections.emptyList());

            mockMvc.perform(get("/notifications")
                            .param("read", "false")
                            .param("type", "FEEDBACK_REPLY")
                            .with(authentication(mockAuthToken)))
                    .andExpect(status().isOk());

            verify(notificationService).getNotificationsByType(any(User.class), eq(false), eq(Notification.Type.FEEDBACK_REPLY));
        }
    }

    @Nested
    @DisplayName("Endpoint: PATCH /{id}/read")
    class MarkAsReadTests {

        @Test
        @DisplayName("Should return no content")
        void markAsRead_ShouldReturnNoContent() throws Exception {
            mockMvc.perform(patch("/notifications/10/read").with(csrf()).with(authentication(mockAuthToken)))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("For a missing or someone else's notification should return not found")
        void markAsRead_WhenNotFound_ShouldReturnNotFound() throws Exception {
            doThrow(new NotificationNotFoundException(99L)).when(notificationService).markAsRead(any(User.class), eq(99L));

            mockMvc.perform(patch("/notifications/99/read").with(csrf()).with(authentication(mockAuthToken)))
                    .andExpect(status().isNotFound());
        }
    }
}

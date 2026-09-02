package com.redcheck.backend;

import com.redcheck.backend.controller.NotificationController;
import com.redcheck.backend.dto.response.NotificationResponseDTO;
import com.redcheck.backend.entity.User;
import com.redcheck.backend.security.JwtService;
import com.redcheck.backend.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
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
            // GIVEN:
            when(notificationService.getNotifications(any(User.class), isNull()))
                    .thenReturn(Collections.singletonList(notificationResponseDTO));

            // WHEN & THEN:
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
            // GIVEN:
            Boolean readParam = false;
            when(notificationService.getNotifications(any(User.class), eq(readParam)))
                    .thenReturn(Collections.singletonList(notificationResponseDTO));

            // WHEN & THEN:
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
}
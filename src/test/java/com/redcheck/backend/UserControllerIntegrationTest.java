package com.redcheck.backend;

import com.redcheck.backend.controller.UserController;
import com.redcheck.backend.dto.request.ChangePasswordRequestDTO;
import com.redcheck.backend.entity.User;
import com.redcheck.backend.exception.DemoAccountRestrictedException;
import com.redcheck.backend.exception.InvalidCurrentPasswordException;
import com.redcheck.backend.security.JwtService;
import com.redcheck.backend.service.UserService;
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import tools.jackson.databind.ObjectMapper;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@DisplayName("Integration Tests - UserController")
public class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

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
    @DisplayName("Endpoint: GET")
    class GetProfileTests {

        @Test
        @DisplayName("When authenticated should return ok and user response DTO")
        void getProfile_ShouldReturnOkAndUserResponseDTO() throws Exception {
            // GIVEN:

            // WHEN & THEN:
            mockMvc.perform(get("/users/profile")
                            .with(authentication(mockAuthToken))
                            .contentType(MediaType.APPLICATION_JSON))

                    .andExpect(status().isOk())
                    .andExpect(MockMvcResultMatchers.content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))

                    .andExpect(MockMvcResultMatchers.jsonPath("$.username").value(mockUser.getActualUsername()))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.hasPassword").value(true));
        }
    }

    @Nested
    @DisplayName("Endpoint: DELETE")
    class DeleteMyAccountTests {

        @Test
        @DisplayName("When authenticated should delete account and return no content")
        void deleteMyAccount_ShouldReturnNoContent() throws Exception {
            // GIVEN:
            doNothing().when(userService).deleteUser(anyString());

            // WHEN & THEN:
            mockMvc.perform(delete("/users/me")
                            .with(csrf())
                            .with(authentication(mockAuthToken))
                            .contentType(MediaType.APPLICATION_JSON))

                    .andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("Endpoint: PATCH /users/me/password")
    class ChangePasswordTests {

        @Test
        @DisplayName("With valid data should return no content")
        void changePassword_WithValidData_ShouldReturnNoContent() throws Exception {
            // GIVEN
            ChangePasswordRequestDTO requestDTO = new ChangePasswordRequestDTO("currentPassword", "newPassword123");
            doNothing().when(userService).changePassword(anyString(), any(ChangePasswordRequestDTO.class));

            // WHEN & THEN
            mockMvc.perform(patch("/users/me/password")
                            .with(csrf())
                            .with(authentication(mockAuthToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDTO)))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("With wrong current password should return bad request")
        void changePassword_WithWrongCurrentPassword_ShouldReturnBadRequest() throws Exception {
            // GIVEN
            ChangePasswordRequestDTO requestDTO = new ChangePasswordRequestDTO("wrongPassword", "newPassword123");
            doThrow(new InvalidCurrentPasswordException())
                    .when(userService).changePassword(anyString(), any(ChangePasswordRequestDTO.class));

            // WHEN & THEN
            mockMvc.perform(patch("/users/me/password")
                            .with(csrf())
                            .with(authentication(mockAuthToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDTO)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("For the demo account should return forbidden")
        void changePassword_ForDemoAccount_ShouldReturnForbidden() throws Exception {
            // GIVEN
            User demoUser = User.builder()
                    .username("demo-es")
                    .email("demo-es@redcheck.com")
                    .password("demoPassword")
                    .build();
            demoUser.setId(2L);
            UsernamePasswordAuthenticationToken demoAuthToken = new UsernamePasswordAuthenticationToken(
                    demoUser,
                    null,
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
            );

            ChangePasswordRequestDTO requestDTO = new ChangePasswordRequestDTO("currentPassword", "newPassword123");
            doThrow(new DemoAccountRestrictedException())
                    .when(userService).changePassword(anyString(), any(ChangePasswordRequestDTO.class));

            // WHEN & THEN
            mockMvc.perform(patch("/users/me/password")
                            .with(csrf())
                            .with(authentication(demoAuthToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDTO)))
                    .andExpect(status().isForbidden());
        }
    }
}
package com.redcheck.backend;

import com.redcheck.backend.controller.UserController;
import com.redcheck.backend.entity.User;
import com.redcheck.backend.security.JwtService;
import com.redcheck.backend.service.UserService;
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

import java.util.Collections;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@DisplayName("Integration Tests - UserController")
public class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

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

                    .andExpect(MockMvcResultMatchers.jsonPath("$.username").value(mockUser.getUsername()));
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
}
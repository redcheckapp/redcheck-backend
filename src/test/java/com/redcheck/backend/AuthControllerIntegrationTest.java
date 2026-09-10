package com.redcheck.backend;

import com.redcheck.backend.controller.AuthController;
import com.redcheck.backend.dto.request.ForgotPasswordRequestDTO;
import com.redcheck.backend.dto.request.GoogleAuthRequestDTO;
import com.redcheck.backend.dto.request.LoginRequestDTO;
import com.redcheck.backend.dto.request.RegisterRequestDTO;
import com.redcheck.backend.dto.request.ResetPasswordRequestDTO;
import com.redcheck.backend.dto.response.AuthResponseDTO;
import com.redcheck.backend.exception.InvalidResetTokenException;
import com.redcheck.backend.security.JwtService;
import com.redcheck.backend.service.AuthService;
import com.redcheck.backend.service.PasswordResetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import tools.jackson.databind.ObjectMapper;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("Integration Tests - AuthController")
public class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private PasswordResetService passwordResetService;

    @MockitoBean
    private JwtService jwtService;

    private RegisterRequestDTO registerRequestDTO;
    private LoginRequestDTO loginRequestDTO;
    private GoogleAuthRequestDTO googleAuthRequestDTO;
    private AuthResponseDTO authResponseDTO;
    private final String MOCK_TOKEN = "mock.jwt.token.123";

    @BeforeEach
    void setUp() {
        registerRequestDTO = RegisterRequestDTO.builder()
                .username("newuser")
                .email("newuser@redcheck.com")
                .password("password123")
                .build();

        loginRequestDTO = LoginRequestDTO.builder()
                .emailOrUsername("newuser@redcheck.com")
                .password("password123")
                .build();

        googleAuthRequestDTO = GoogleAuthRequestDTO.builder()
                .idToken("mock.id.token")
                .build();

        authResponseDTO = AuthResponseDTO.builder()
                .token(MOCK_TOKEN)
                .build();
    }

    @Nested
    @DisplayName("Endpoint: POST /auth/register")
    class RegisterTests {

        @Test
        @DisplayName("With valid data should return ok status and token")
        void register_WithValidData_ShouldReturnOkAndToken() throws Exception {
            when(authService.register(any(RegisterRequestDTO.class)))
                    .thenReturn(authResponseDTO);

            String jsonRequest = objectMapper.writeValueAsString(registerRequestDTO);

            mockMvc.perform(post("/auth/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest))
                    .andExpect(status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.token").value(MOCK_TOKEN));
        }
    }

    @Nested
    @DisplayName("Endpoint: POST /auth/login")
    class LoginTests {

        @Test
        @DisplayName("With valid credentials should return ok status and token")
        void login_WithValidCredentials_ShouldReturnOkAndToken() throws Exception {
            when(authService.login(any(LoginRequestDTO.class)))
                    .thenReturn(authResponseDTO);

            String jsonRequest = objectMapper.writeValueAsString(loginRequestDTO);

            mockMvc.perform(post("/auth/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest))
                    .andExpect(status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.token").value(MOCK_TOKEN));
        }
    }

    @Nested
    @DisplayName("Endpoint: POST /auth/google")
    class GoogleTests {

        @Test
        @DisplayName("With a valid Google id token should return ok status and token")
        void loginWithGoogle_WithValidIdToken_ShouldReturnOkAndToken() throws Exception {
            when(authService.loginWithGoogle(any(GoogleAuthRequestDTO.class)))
                    .thenReturn(authResponseDTO);

            String jsonRequest = objectMapper.writeValueAsString(googleAuthRequestDTO);

            mockMvc.perform(post("/auth/google")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest))
                    .andExpect(status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.token").value(MOCK_TOKEN));
        }
    }

    @Nested
    @DisplayName("Endpoint: POST /auth/forgot-password")
    class ForgotPasswordTests {

        @Test
        @DisplayName("Should always return ok, regardless of whether the email is registered")
        void forgotPassword_ShouldAlwaysReturnOk() throws Exception {
            doNothing().when(passwordResetService).forgotPassword(anyString(), any());

            ForgotPasswordRequestDTO requestDTO = ForgotPasswordRequestDTO.builder()
                    .email("someone@redcheck.com")
                    .lang("es")
                    .build();

            mockMvc.perform(post("/auth/forgot-password")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDTO)))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Endpoint: POST /auth/reset-password")
    class ResetPasswordTests {

        @Test
        @DisplayName("With a valid token should return no content")
        void resetPassword_WithValidToken_ShouldReturnNoContent() throws Exception {
            doNothing().when(passwordResetService).resetPassword(anyString(), anyString());

            ResetPasswordRequestDTO requestDTO = ResetPasswordRequestDTO.builder()
                    .token("valid-token")
                    .newPassword("newPassword123")
                    .build();

            mockMvc.perform(post("/auth/reset-password")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDTO)))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("With an invalid or expired token should return bad request")
        void resetPassword_WithInvalidToken_ShouldReturnBadRequest() throws Exception {
            doThrow(new InvalidResetTokenException())
                    .when(passwordResetService).resetPassword(anyString(), anyString());

            ResetPasswordRequestDTO requestDTO = ResetPasswordRequestDTO.builder()
                    .token("expired-token")
                    .newPassword("newPassword123")
                    .build();

            mockMvc.perform(post("/auth/reset-password")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDTO)))
                    .andExpect(status().isBadRequest());
        }
    }
}
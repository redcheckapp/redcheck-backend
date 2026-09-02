package com.redcheck.backend;

import com.redcheck.backend.controller.AuthController;
import com.redcheck.backend.dto.request.LoginRequestDTO;
import com.redcheck.backend.dto.request.RegisterRequestDTO;
import com.redcheck.backend.dto.response.AuthResponseDTO;
import com.redcheck.backend.security.JwtService;
import com.redcheck.backend.service.AuthService;
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
    private JwtService jwtService;

    private RegisterRequestDTO registerRequestDTO;
    private LoginRequestDTO loginRequestDTO;
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
                .email("newuser@redcheck.com")
                .password("password123")
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
}
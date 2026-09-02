package com.redcheck.backend;

import com.redcheck.backend.dto.request.LoginRequestDTO;
import com.redcheck.backend.dto.request.RegisterRequestDTO;
import com.redcheck.backend.dto.response.AuthResponseDTO;
import com.redcheck.backend.entity.User;
import com.redcheck.backend.repository.UserRepository;
import com.redcheck.backend.security.JwtService;
import com.redcheck.backend.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unitary Tests - AuthService")
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private User mockUser;
    private RegisterRequestDTO mockRegisterRequest;
    private LoginRequestDTO mockLoginRequest;
    private final String MOCK_TOKEN = "jwt.fake.token";

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@redcheck.com")
                .password("encodedPassword") // El password ya codificado
                .build();

        mockRegisterRequest = RegisterRequestDTO.builder()
                .username("testuser")
                .email("test@redcheck.com")
                .password("rawPassword123") // El password sin codificar
                .build();

        mockLoginRequest = LoginRequestDTO.builder()
                .email("test@redcheck.com")
                .password("rawPassword123")
                .build();
    }

    @Nested
    @DisplayName("Method: register")
    class RegisterTests {

        @Test
        @DisplayName("When data is valid should register user and return token")
        void register_WhenDataIsValid_ShouldRegisterUserAndReturnToken() {
            // GIVEN
            when(userRepository.existsByEmail(mockRegisterRequest.email()))
                    .thenReturn(false);

            when(jwtService.generateToken(mockUser))
                    .thenReturn(MOCK_TOKEN);

            // WHEN
            AuthResponseDTO result = authService.register(mockRegisterRequest);

            // THEN
            assertNotNull(result);
            assertEquals(MOCK_TOKEN, result.token());
            verify(userRepository, times(1)).existsByEmail(mockRegisterRequest.email());
            verify(jwtService, times(1)).generateToken(mockUser);
            verify(userRepository, times(1)).save(any(User.class));
        }

        @Test
        @DisplayName("When email already exists should throw exception")
        void register_WhenEmailExists_ShouldThrowException() {
            // GIVEN
            when(userRepository.existsByEmail(mockRegisterRequest.email()))
                    .thenReturn(true);

            // WHEN
            assertThrows(RuntimeException.class, () -> {
                authService.register(mockRegisterRequest);
            });

            // THEN
            verify(userRepository, times(1)).existsByEmail(mockRegisterRequest.email());
            verify(jwtService, never()).generateToken(mockUser);
            verify(userRepository, never()).save(any(User.class));        }
    }

    @Nested
    @DisplayName("Method: login")
    class LoginTests {

        @Test
        @DisplayName("When credentials are valid should authenticate and return token")
        void login_WhenCredentialsAreValid_ShouldAuthenticateAndReturnToken() {
            // GIVEN
            when(userRepository.findByEmail(any()))
                    .thenReturn(Optional.of(mockUser));

            when(jwtService.generateToken(eq(mockUser)))
                    .thenReturn(MOCK_TOKEN);

            // WHEN
            AuthResponseDTO result = authService.login(mockLoginRequest);

            // THEN
            assertNotNull(result);
            assertEquals(MOCK_TOKEN, result.token());
            verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
            verify(userRepository, times(1)).findByEmail(mockRegisterRequest.email());
            verify(jwtService, times(1)).generateToken(mockUser);
        }

        @Test
        @DisplayName("When user does not exist should throw exception")
        void login_WhenUserDoesNotExist_ShouldThrowException() {
            // GIVEN
            when(userRepository.findByEmail(any()))
                    .thenReturn(Optional.empty());

            // WHEN
            assertThrows(NoSuchElementException.class, () -> {
                authService.login(mockLoginRequest);
            });

            // THEN
            verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
            verify(userRepository, times(1)).findByEmail(any());
            verify(jwtService, never()).generateToken(any());
        }
    }
}
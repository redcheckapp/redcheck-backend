package com.redcheck.backend;

import com.redcheck.backend.dto.request.GoogleAuthRequestDTO;
import com.redcheck.backend.dto.request.LoginRequestDTO;
import com.redcheck.backend.dto.request.RegisterRequestDTO;
import com.redcheck.backend.dto.response.AuthResponseDTO;
import com.redcheck.backend.entity.User;
import com.redcheck.backend.exception.InvalidGoogleTokenException;
import com.redcheck.backend.repository.UserRepository;
import com.redcheck.backend.security.GoogleTokenVerifierService;
import com.redcheck.backend.security.GoogleUserInfo;
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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

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

    @Mock
    private GoogleTokenVerifierService googleTokenVerifierService;

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
                .emailOrUsername("test@redcheck.com")
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
            when(userRepository.existsByUsername(mockRegisterRequest.username()))
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

        @Test
        @DisplayName("When username already exists should throw exception")
        void register_WhenUsernameExists_ShouldThrowException() {
            // GIVEN
            when(userRepository.existsByEmail(mockRegisterRequest.email()))
                    .thenReturn(false);
            when(userRepository.existsByUsername(mockRegisterRequest.username()))
                    .thenReturn(true);

            // WHEN
            assertThrows(RuntimeException.class, () -> {
                authService.register(mockRegisterRequest);
            });

            // THEN
            verify(userRepository, times(1)).existsByUsername(mockRegisterRequest.username());
            verify(jwtService, never()).generateToken(mockUser);
            verify(userRepository, never()).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("Method: login")
    class LoginTests {

        @Test
        @DisplayName("When credentials are valid should authenticate and return token")
        void login_WhenCredentialsAreValid_ShouldAuthenticateAndReturnToken() {
            // GIVEN
            when(userRepository.findByEmailOrUsername(mockLoginRequest.emailOrUsername()))
                    .thenReturn(Optional.of(mockUser));

            when(jwtService.generateToken(eq(mockUser)))
                    .thenReturn(MOCK_TOKEN);

            // WHEN
            AuthResponseDTO result = authService.login(mockLoginRequest);

            // THEN
            assertNotNull(result);
            assertEquals(MOCK_TOKEN, result.token());
            verify(userRepository, times(1)).findByEmailOrUsername(mockLoginRequest.emailOrUsername());
            verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
            verify(jwtService, times(1)).generateToken(mockUser);
        }

        @Test
        @DisplayName("When logging in with a username instead of an email should still authenticate")
        void login_WithUsername_ShouldAuthenticateAndReturnToken() {
            // GIVEN
            LoginRequestDTO usernameLoginRequest = LoginRequestDTO.builder()
                    .emailOrUsername(mockUser.getActualUsername())
                    .password("rawPassword123")
                    .build();

            when(userRepository.findByEmailOrUsername(mockUser.getActualUsername()))
                    .thenReturn(Optional.of(mockUser));
            when(jwtService.generateToken(eq(mockUser)))
                    .thenReturn(MOCK_TOKEN);

            // WHEN
            AuthResponseDTO result = authService.login(usernameLoginRequest);

            // THEN
            assertNotNull(result);
            assertEquals(MOCK_TOKEN, result.token());
            // Authentication always proceeds with the resolved email, even
            // though a username was supplied — see AuthService#login.
            verify(authenticationManager, times(1)).authenticate(
                    argThat(token -> mockUser.getEmail().equals(token.getPrincipal()))
            );
        }

        @Test
        @DisplayName("When no user matches the identifier should throw exception without authenticating")
        void login_WhenUserDoesNotExist_ShouldThrowExceptionWithoutAuthenticating() {
            // GIVEN
            when(userRepository.findByEmailOrUsername(any()))
                    .thenReturn(Optional.empty());

            // WHEN
            assertThrows(BadCredentialsException.class, () -> {
                authService.login(mockLoginRequest);
            });

            // THEN
            verify(userRepository, times(1)).findByEmailOrUsername(any());
            verify(authenticationManager, never()).authenticate(any());
            verify(jwtService, never()).generateToken(any());
        }
    }

    @Nested
    @DisplayName("Method: loginWithGoogle")
    class LoginWithGoogleTests {

        private final GoogleAuthRequestDTO mockGoogleRequest =
                GoogleAuthRequestDTO.builder().idToken("mock.id.token").build();

        @Test
        @DisplayName("When the Google id already belongs to a user should authenticate and return token")
        void loginWithGoogle_WhenGoogleIdMatchesExistingUser_ShouldReturnToken() {
            // GIVEN
            GoogleUserInfo googleUserInfo = new GoogleUserInfo("google-sub-1", mockUser.getEmail(), true, "Test User");
            when(googleTokenVerifierService.verify(mockGoogleRequest.idToken()))
                    .thenReturn(googleUserInfo);
            when(userRepository.findByGoogleId("google-sub-1"))
                    .thenReturn(Optional.of(mockUser));
            when(jwtService.generateToken(mockUser))
                    .thenReturn(MOCK_TOKEN);

            // WHEN
            AuthResponseDTO result = authService.loginWithGoogle(mockGoogleRequest);

            // THEN
            assertNotNull(result);
            assertEquals(MOCK_TOKEN, result.token());
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("When a password account already exists with the same email should link the Google id")
        void loginWithGoogle_WhenEmailMatchesExistingUser_ShouldLinkGoogleIdAndReturnToken() {
            // GIVEN
            GoogleUserInfo googleUserInfo = new GoogleUserInfo("google-sub-2", mockUser.getEmail(), true, "Test User");
            when(googleTokenVerifierService.verify(mockGoogleRequest.idToken()))
                    .thenReturn(googleUserInfo);
            when(userRepository.findByGoogleId("google-sub-2"))
                    .thenReturn(Optional.empty());
            when(userRepository.findByEmail(mockUser.getEmail()))
                    .thenReturn(Optional.of(mockUser));
            when(userRepository.save(mockUser))
                    .thenReturn(mockUser);
            when(jwtService.generateToken(mockUser))
                    .thenReturn(MOCK_TOKEN);

            // WHEN
            AuthResponseDTO result = authService.loginWithGoogle(mockGoogleRequest);

            // THEN
            assertNotNull(result);
            assertEquals(MOCK_TOKEN, result.token());
            assertEquals("google-sub-2", mockUser.getGoogleId());
            verify(userRepository, times(1)).save(mockUser);
        }

        @Test
        @DisplayName("When no user matches should create a new user and return token")
        void loginWithGoogle_WhenNewUser_ShouldCreateUserAndReturnToken() {
            // GIVEN
            GoogleUserInfo googleUserInfo = new GoogleUserInfo("google-sub-3", "brand.new@redcheck.com", true, "Brand New");
            when(googleTokenVerifierService.verify(mockGoogleRequest.idToken()))
                    .thenReturn(googleUserInfo);
            when(userRepository.findByGoogleId("google-sub-3"))
                    .thenReturn(Optional.empty());
            when(userRepository.findByEmail("brand.new@redcheck.com"))
                    .thenReturn(Optional.empty());
            when(userRepository.existsByUsername(any()))
                    .thenReturn(false);
            when(userRepository.save(any(User.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(jwtService.generateToken(any(User.class)))
                    .thenReturn(MOCK_TOKEN);

            // WHEN
            AuthResponseDTO result = authService.loginWithGoogle(mockGoogleRequest);

            // THEN
            assertNotNull(result);
            assertEquals(MOCK_TOKEN, result.token());
            verify(userRepository, times(1)).save(argThat(user ->
                    "brandnew".equals(user.getActualUsername())
                            && "google-sub-3".equals(user.getGoogleId())
                            && user.getPassword() == null
            ));
        }

        @Test
        @DisplayName("When Google reports an unverified email should throw exception")
        void loginWithGoogle_WhenEmailNotVerified_ShouldThrowException() {
            // GIVEN
            GoogleUserInfo googleUserInfo = new GoogleUserInfo("google-sub-4", "unverified@redcheck.com", false, "Unverified");
            when(googleTokenVerifierService.verify(mockGoogleRequest.idToken()))
                    .thenReturn(googleUserInfo);

            // WHEN
            assertThrows(InvalidGoogleTokenException.class, () -> {
                authService.loginWithGoogle(mockGoogleRequest);
            });

            // THEN
            verify(userRepository, never()).findByGoogleId(any());
            verify(jwtService, never()).generateToken(any());
        }
    }
}
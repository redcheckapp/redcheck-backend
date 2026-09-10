package com.redcheck.backend;

import com.redcheck.backend.entity.PasswordResetToken;
import com.redcheck.backend.entity.User;
import com.redcheck.backend.exception.InvalidResetTokenException;
import com.redcheck.backend.repository.PasswordResetTokenRepository;
import com.redcheck.backend.repository.UserRepository;
import com.redcheck.backend.service.EmailService;
import com.redcheck.backend.service.PasswordResetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unitary Tests - PasswordResetService")
public class PasswordResetServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private PasswordResetService passwordResetService;

    private User mockUser;
    private final String userEmail = "user@redcheck.com";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(passwordResetService, "frontendUrl", "http://localhost:5173");

        mockUser = User.builder()
                .username("user")
                .email(userEmail)
                .password("encodedCurrentPassword")
                .build();
        mockUser.setId(1L);
    }

    @Nested
    @DisplayName("Method: forgotPassword")
    class ForgotPasswordTests {

        @Test
        @DisplayName("When the user exists should issue a token and send the email")
        void forgotPassword_WhenUserExists_ShouldIssueTokenAndSendEmail() {
            when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(mockUser));

            passwordResetService.forgotPassword(userEmail, "es");

            verify(passwordResetTokenRepository, times(1)).deleteUnusedByUser(mockUser);
            verify(passwordResetTokenRepository, times(1)).save(any(PasswordResetToken.class));
            verify(emailService, times(1)).sendPasswordResetEmail(eq(userEmail), anyString(), eq("es"));
        }

        @Test
        @DisplayName("When the user does not exist should do nothing")
        void forgotPassword_WhenUserDoesNotExist_ShouldDoNothing() {
            when(userRepository.findByEmail(userEmail)).thenReturn(Optional.empty());

            passwordResetService.forgotPassword(userEmail, "es");

            verify(passwordResetTokenRepository, never()).save(any(PasswordResetToken.class));
            verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("When the account is Google-only (no password) should do nothing")
        void forgotPassword_WhenGoogleOnlyAccount_ShouldDoNothing() {
            mockUser.setPassword(null);
            when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(mockUser));

            passwordResetService.forgotPassword(userEmail, "es");

            verify(passwordResetTokenRepository, never()).save(any(PasswordResetToken.class));
            verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString(), any());
        }
    }

    @Nested
    @DisplayName("Method: resetPassword")
    class ResetPasswordTests {

        @Test
        @DisplayName("With a valid, unexpired token should update the password and mark the token used")
        void resetPassword_WithValidToken_ShouldUpdatePasswordAndMarkTokenUsed() {
            PasswordResetToken token = PasswordResetToken.builder()
                    .token("valid-token")
                    .user(mockUser)
                    .expiryDate(LocalDateTime.now().plusMinutes(10))
                    .used(false)
                    .build();
            when(passwordResetTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));
            when(passwordEncoder.encode("newPassword123")).thenReturn("encodedNewPassword");

            passwordResetService.resetPassword("valid-token", "newPassword123");

            assertEquals("encodedNewPassword", mockUser.getPassword());
            verify(userRepository, times(1)).save(mockUser);

            ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
            verify(passwordResetTokenRepository, times(1)).save(tokenCaptor.capture());
            assertEquals(true, tokenCaptor.getValue().isUsed());
        }

        @Test
        @DisplayName("With an unknown token should throw InvalidResetTokenException")
        void resetPassword_WithUnknownToken_ShouldThrowException() {
            when(passwordResetTokenRepository.findByToken("unknown-token")).thenReturn(Optional.empty());

            assertThrows(InvalidResetTokenException.class, () ->
                    passwordResetService.resetPassword("unknown-token", "newPassword123"));

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("With an expired token should throw InvalidResetTokenException")
        void resetPassword_WithExpiredToken_ShouldThrowException() {
            PasswordResetToken token = PasswordResetToken.builder()
                    .token("expired-token")
                    .user(mockUser)
                    .expiryDate(LocalDateTime.now().minusMinutes(1))
                    .used(false)
                    .build();
            when(passwordResetTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(token));

            assertThrows(InvalidResetTokenException.class, () ->
                    passwordResetService.resetPassword("expired-token", "newPassword123"));

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("With an already-used token should throw InvalidResetTokenException")
        void resetPassword_WithUsedToken_ShouldThrowException() {
            PasswordResetToken token = PasswordResetToken.builder()
                    .token("used-token")
                    .user(mockUser)
                    .expiryDate(LocalDateTime.now().plusMinutes(10))
                    .used(true)
                    .build();
            when(passwordResetTokenRepository.findByToken("used-token")).thenReturn(Optional.of(token));

            assertThrows(InvalidResetTokenException.class, () ->
                    passwordResetService.resetPassword("used-token", "newPassword123"));

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("With a too-short new password should throw IllegalArgumentException")
        void resetPassword_WithTooShortPassword_ShouldThrowException() {
            assertThrows(IllegalArgumentException.class, () ->
                    passwordResetService.resetPassword("valid-token", "short"));

            verify(passwordResetTokenRepository, never()).findByToken(anyString());
        }
    }
}

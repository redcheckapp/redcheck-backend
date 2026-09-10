package com.redcheck.backend;

import com.redcheck.backend.dto.request.ChangePasswordRequestDTO;
import com.redcheck.backend.entity.User;
import com.redcheck.backend.exception.InvalidCurrentPasswordException;
import com.redcheck.backend.repository.UserRepository;
import com.redcheck.backend.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unitary Tests - UserService")
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User mockUser;
    private String userEmail;

    @BeforeEach
    void setUp() {
        userEmail = "user@redcheck.com";

        mockUser = User.builder()
                .username("user")
                .email(userEmail)
                .password("encodedCurrentPassword")
                .build();
        mockUser.setId(1L);
    }

    @Nested
    @DisplayName("Method: deleteUser")
    class DeleteUserTests {

        @Test
        @DisplayName("When user exists should delete user")
        void deleteUser_WhenUserExists_ShouldDeleteUser() {
            // GIVEN
            when(userRepository.findByEmail(userEmail))
                    .thenReturn(Optional.of(mockUser));

            // WHEN
            userService.deleteUser(userEmail);

            // THEN
            verify(userRepository, times(1)).findByEmail(userEmail);
            verify(userRepository, times(1)).delete(mockUser);
        }

        @Test
        @DisplayName("When user does not exist should throw exception")
        void deleteUser_WhenUserDoesNotExist_ShouldThrowException() {
            // GIVEN
            when(userRepository.findByEmail(userEmail))
                    .thenReturn(Optional.empty());

            // WHEN
            assertThrows(RuntimeException.class, () -> {
                userService.deleteUser(userEmail);
            });

            // THEN
            verify(userRepository, times(1)).findByEmail(userEmail);
            verify(userRepository, never()).delete(any(User.class));
        }
    }

    @Nested
    @DisplayName("Method: changePassword")
    class ChangePasswordTests {

        @Test
        @DisplayName("When current password matches should encode and save the new password")
        void changePassword_WhenCurrentPasswordMatches_ShouldUpdatePassword() {
            // GIVEN
            ChangePasswordRequestDTO requestDTO = new ChangePasswordRequestDTO("currentPassword", "newPassword123");
            when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(mockUser));
            when(passwordEncoder.matches("currentPassword", "encodedCurrentPassword")).thenReturn(true);
            when(passwordEncoder.encode("newPassword123")).thenReturn("encodedNewPassword");

            // WHEN
            userService.changePassword(userEmail, requestDTO);

            // THEN
            assertEquals("encodedNewPassword", mockUser.getPassword());
            verify(userRepository, times(1)).save(mockUser);
        }

        @Test
        @DisplayName("When current password is wrong should throw InvalidCurrentPasswordException")
        void changePassword_WhenCurrentPasswordIsWrong_ShouldThrowException() {
            // GIVEN
            ChangePasswordRequestDTO requestDTO = new ChangePasswordRequestDTO("wrongPassword", "newPassword123");
            when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(mockUser));
            when(passwordEncoder.matches("wrongPassword", "encodedCurrentPassword")).thenReturn(false);

            // WHEN & THEN
            assertThrows(InvalidCurrentPasswordException.class, () -> {
                userService.changePassword(userEmail, requestDTO);
            });

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("When account has no password (Google-only) should set it without checking current password")
        void changePassword_WhenGoogleOnlyAccount_ShouldSetPasswordWithoutCheck() {
            // GIVEN
            mockUser.setPassword(null);
            ChangePasswordRequestDTO requestDTO = new ChangePasswordRequestDTO(null, "newPassword123");
            when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(mockUser));
            when(passwordEncoder.encode("newPassword123")).thenReturn("encodedNewPassword");

            // WHEN
            userService.changePassword(userEmail, requestDTO);

            // THEN
            assertEquals("encodedNewPassword", mockUser.getPassword());
            verify(userRepository, times(1)).save(mockUser);
        }

        @Test
        @DisplayName("When new password is too short should throw IllegalArgumentException")
        void changePassword_WhenNewPasswordTooShort_ShouldThrowException() {
            // GIVEN
            ChangePasswordRequestDTO requestDTO = new ChangePasswordRequestDTO("currentPassword", "short");

            // WHEN & THEN
            assertThrows(IllegalArgumentException.class, () -> {
                userService.changePassword(userEmail, requestDTO);
            });

            verify(userRepository, never()).findByEmail(any());
            verify(userRepository, never()).save(any(User.class));
        }
    }
}
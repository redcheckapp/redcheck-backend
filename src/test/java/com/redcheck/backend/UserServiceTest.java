package com.redcheck.backend;

import com.redcheck.backend.entity.User;
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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unitary Tests - UserService")
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

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
                .password("redcheckUser")
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
}
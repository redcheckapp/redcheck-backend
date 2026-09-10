package com.redcheck.backend.service;

import com.redcheck.backend.dto.request.ChangePasswordRequestDTO;
import com.redcheck.backend.entity.User;
import com.redcheck.backend.exception.InvalidCurrentPasswordException;
import com.redcheck.backend.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void deleteUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        userRepository.delete(user);
    }

    @Transactional
    public void changePassword(String email, ChangePasswordRequestDTO requestDTO) {
        if (requestDTO.newPassword() == null || requestDTO.newPassword().length() < 8) {
            throw new IllegalArgumentException("New password must be at least 8 characters long");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // A Google-only account (password == null) has nothing to verify
        // against — this is "set a password for the first time", not
        // "change it", so no current-password check applies there.
        if (user.getPassword() != null
                && (requestDTO.currentPassword() == null
                    || !passwordEncoder.matches(requestDTO.currentPassword(), user.getPassword()))) {
            throw new InvalidCurrentPasswordException();
        }

        user.setPassword(passwordEncoder.encode(requestDTO.newPassword()));
        userRepository.save(user);
    }
}
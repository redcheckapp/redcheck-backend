package com.redcheck.backend.service;

import com.redcheck.backend.entity.PasswordResetToken;
import com.redcheck.backend.entity.User;
import com.redcheck.backend.exception.InvalidResetTokenException;
import com.redcheck.backend.repository.PasswordResetTokenRepository;
import com.redcheck.backend.repository.UserRepository;
import com.redcheck.backend.util.DemoAccountUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final int TOKEN_EXPIRY_MINUTES = 30;
    private static final int NEW_PASSWORD_MIN_LENGTH = 8;

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Transactional
    public void forgotPassword(String email, String lang) {
        userRepository.findByEmail(email).ifPresent(user -> {
            // A Google-only account (no password) has nothing to reset —
            // sending a link there would be misleading. The controller
            // returns the same generic response either way, so this is not
            // observable from outside and doesn't leak account state.
            if (user.getPassword() == null) {
                return;
            }

            // Same shared-identity protection as UserService#changePassword
            // — a reset link would let anyone lock every other demo user
            // out. Silently no-op'd, same reasoning as above: the generic
            // response never varies, so this isn't observable either.
            if (DemoAccountUtils.isDemoAccount(user.getEmail())) {
                return;
            }

            passwordResetTokenRepository.deleteUnusedByUser(user);

            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .token(UUID.randomUUID().toString())
                    .user(user)
                    .expiryDate(LocalDateTime.now().plusMinutes(TOKEN_EXPIRY_MINUTES))
                    .build();
            passwordResetTokenRepository.save(resetToken);

            String resetLink = frontendUrl + "/reset-password?token=" + resetToken.getToken();
            emailService.sendPasswordResetEmail(user.getEmail(), resetLink, lang);
        });
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        if (newPassword == null || newPassword.length() < NEW_PASSWORD_MIN_LENGTH) {
            throw new IllegalArgumentException("New password must be at least 8 characters long");
        }

        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(InvalidResetTokenException::new);

        // Defense in depth: forgotPassword already refuses to issue a token
        // for a demo account, but a token issued before that guard existed
        // (or reached some other way) must still be rejected here — treated
        // as just another invalid token, not a distinct error, so this
        // never reveals anything about the account behind it either.
        if (resetToken.isUsed() || resetToken.isExpired() || DemoAccountUtils.isDemoAccount(resetToken.getUser().getEmail())) {
            throw new InvalidResetTokenException();
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);
    }
}

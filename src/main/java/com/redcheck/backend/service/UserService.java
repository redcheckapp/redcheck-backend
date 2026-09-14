package com.redcheck.backend.service;

import com.redcheck.backend.dto.request.ChangePasswordRequestDTO;
import com.redcheck.backend.dto.request.UpdateAliasRequestDTO;
import com.redcheck.backend.entity.User;
import com.redcheck.backend.exception.DemoAccountRestrictedException;
import com.redcheck.backend.exception.InvalidCurrentPasswordException;
import com.redcheck.backend.exception.NewPasswordSameAsCurrentException;
import com.redcheck.backend.repository.UserRepository;
import com.redcheck.backend.util.DemoAccountUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final int ALIAS_MAX_LENGTH = 30;

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
        // Guards the same shared demo identities deleteMyAccount protects
        // in UserController — anyone could otherwise lock every other demo
        // user out by changing the one password they all rely on.
        if (DemoAccountUtils.isDemoAccount(email)) {
            throw new DemoAccountRestrictedException();
        }

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

        // Only meaningful when there's an existing password to compare
        // against — a Google-only account "setting" its first password has
        // nothing to collide with.
        if (user.getPassword() != null && passwordEncoder.matches(requestDTO.newPassword(), user.getPassword())) {
            throw new NewPasswordSameAsCurrentException();
        }

        user.setPassword(passwordEncoder.encode(requestDTO.newPassword()));
        userRepository.save(user);
    }

    @Transactional
    public void updateAlias(String email, UpdateAliasRequestDTO requestDTO) {
        // Same shared-identity reasoning as changePassword above: the two
        // seeded demo accounts are public and shared, so letting anyone
        // change the alias every visitor sees on that account isn't
        // acceptable either, even though an alias is otherwise harmless.
        if (DemoAccountUtils.isDemoAccount(email)) {
            throw new DemoAccountRestrictedException();
        }

        String alias = requestDTO.alias() == null ? null : requestDTO.alias().trim();
        if (alias != null && alias.length() > ALIAS_MAX_LENGTH) {
            throw new IllegalArgumentException("Alias must be at most " + ALIAS_MAX_LENGTH + " characters long");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // A blank alias resets to the default (the username itself, see
        // User#getDisplayAlias) rather than being rejected as invalid — this
        // is how a user "un-sets" a custom alias without deleting anything.
        user.setAlias((alias == null || alias.isBlank()) ? null : alias);
        userRepository.save(user);
    }
}
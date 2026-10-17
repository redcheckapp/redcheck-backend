package com.redcheck.backend.service;

import com.redcheck.backend.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

// Single source of truth for "who can open the developer dashboard".
// Deliberately an env-var allowlist (ADMIN_EMAILS) rather than a role column
// on User: there's exactly one admin, so a column would mean a migration plus
// a way to grant it, for no real gain. SecurityConfig gates /admin/** through
// this, and UserController exposes it as UserResponseDTO.isAdmin so the
// frontend knows whether to show the entry point — that flag is cosmetic,
// the security rule is what actually enforces access.
@Service
public class AdminAccessService {

    private final Set<String> adminEmails;

    public AdminAccessService(@Value("${app.admin.emails:}") List<String> adminEmails) {
        this.adminEmails = adminEmails.stream()
                .map(String::trim)
                .filter(email -> !email.isEmpty())
                .map(email -> email.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean isAdmin(User user) {
        return user != null
                && user.getEmail() != null
                && adminEmails.contains(user.getEmail().toLowerCase(Locale.ROOT));
    }

    public Set<String> getAdminEmails() {
        return adminEmails;
    }
}

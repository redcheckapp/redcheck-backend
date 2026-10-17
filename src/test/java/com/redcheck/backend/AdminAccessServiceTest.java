package com.redcheck.backend;

import com.redcheck.backend.entity.User;
import com.redcheck.backend.service.AdminAccessService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Unitary Tests - AdminAccessService")
public class AdminAccessServiceTest {

    private static User userWithEmail(String email) {
        return User.builder().username("user").email(email).build();
    }

    @Test
    @DisplayName("A listed email should be admin, ignoring case and whitespace")
    void isAdmin_ForListedEmail_ShouldReturnTrue() {
        AdminAccessService service = new AdminAccessService(List.of(" Dev@RedCheck.com "));

        assertTrue(service.isAdmin(userWithEmail("dev@redcheck.com")));
        assertTrue(service.isAdmin(userWithEmail("DEV@redcheck.com")));
    }

    @Test
    @DisplayName("An unlisted email should not be admin")
    void isAdmin_ForUnlistedEmail_ShouldReturnFalse() {
        AdminAccessService service = new AdminAccessService(List.of("dev@redcheck.com"));

        assertFalse(service.isAdmin(userWithEmail("user@redcheck.com")));
        assertFalse(service.isAdmin(null));
    }

    @Test
    @DisplayName("An empty config should make nobody admin")
    void isAdmin_WithEmptyConfig_ShouldReturnFalse() {
        AdminAccessService service = new AdminAccessService(List.of(""));

        assertFalse(service.isAdmin(userWithEmail("")));
        assertEquals(Set.of(), service.getAdminEmails());
    }
}

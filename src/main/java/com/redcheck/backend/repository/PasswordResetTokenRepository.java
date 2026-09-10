package com.redcheck.backend.repository;

import com.redcheck.backend.entity.PasswordResetToken;
import com.redcheck.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByToken(String token);

    // Invalidates any outstanding link for this user before issuing a new
    // one, so only the most recently requested reset link ever works.
    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.user = :user AND t.used = false")
    void deleteUnusedByUser(User user);

    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.expiryDate < :cutoffDate OR t.used = true")
    void deleteExpiredOrUsed(LocalDateTime cutoffDate);
}

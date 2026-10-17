package com.redcheck.backend.repository;

import com.redcheck.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // Targeted UPDATE instead of save(): runs on authenticated requests, so
    // it must not rewrite (or merge) the whole User row.
    @Modifying
    @Query("UPDATE User u SET u.lastSeenAt = :now WHERE u.id = :id")
    int updateLastSeenAt(@Param("id") Long id, @Param("now") LocalDateTime now);

    Optional<User> findByEmail(String email);

    Optional<User> findByGoogleId(String googleId);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    // Login accepts either identifier — one query, one input, matched
    // against both columns (username already has a unique DB constraint,
    // same as email, so at most one row can ever match).
    @Query("SELECT u FROM User u WHERE u.email = :identifier OR u.username = :identifier")
    Optional<User> findByEmailOrUsername(@Param("identifier") String identifier);
}
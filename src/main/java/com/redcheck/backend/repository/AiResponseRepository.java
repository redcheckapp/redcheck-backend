package com.redcheck.backend.repository;

import com.redcheck.backend.entity.AiResponse;
import com.redcheck.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface AiResponseRepository extends JpaRepository<AiResponse, Long> {
    Optional<AiResponse> findFirstByUserAndTypeAndCreatedAtOrderByCreatedAtDesc(User user, String type, LocalDate date);
}

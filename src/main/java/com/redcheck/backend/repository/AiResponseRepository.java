package com.redcheck.backend.repository;

import com.redcheck.backend.entity.AiResponse;
import com.redcheck.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AiResponseRepository extends JpaRepository<AiResponse, Long> {

    Optional<AiResponse> findFirstByUserAndTypeOrderByCreatedDateDesc(User user, AiResponse.Type type);
}
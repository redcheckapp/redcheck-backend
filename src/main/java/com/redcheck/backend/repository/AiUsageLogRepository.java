package com.redcheck.backend.repository;

import com.redcheck.backend.entity.AiUsageLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AiUsageLogRepository extends JpaRepository<AiUsageLog, Long> {

    List<AiUsageLog> findAllByCreatedDateGreaterThanEqual(LocalDateTime from);
}

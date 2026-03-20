package com.redcheck.backend.repository;

import com.redcheck.backend.entity.ProgressRecord;
import com.redcheck.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ProgressRecordRepository extends JpaRepository<ProgressRecord, Long> {

    // Heatmap query --> last year
    List<ProgressRecord> findAllByUserAndDateBetweenOrderByDateAsc(User user, LocalDate from, LocalDate to);

    // Daily progress circle
    Optional<ProgressRecord> findByUserAndDate(User user, LocalDate date);

    // Avoid duplicates generating daily record
    boolean existsByUserAndDate(User user, LocalDate date);
}

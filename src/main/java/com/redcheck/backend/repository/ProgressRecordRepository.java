package com.redcheck.backend.repository;

import com.redcheck.backend.entity.ProgressRecord;
import com.redcheck.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ProgressRecordRepository extends JpaRepository<ProgressRecord, Long> {

    // Retrieve progress records for the heatmap (default: last year)
    List<ProgressRecord> findAllByUserAndDateBetweenOrderByDateAsc(User user, LocalDate from, LocalDate to);

    // Find the progress record for a specific date
    Optional<ProgressRecord> findByUserAndDate(User user, LocalDate date);

    // Check if a daily progress record already exists for the given date
    boolean existsByUserAndDate(User user, LocalDate date);
}
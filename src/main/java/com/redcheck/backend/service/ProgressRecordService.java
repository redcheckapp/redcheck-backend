package com.redcheck.backend.service;

import com.redcheck.backend.dto.response.ProgressRecordResponseDTO;
import com.redcheck.backend.entity.ProgressRecord;
import com.redcheck.backend.entity.User;
import com.redcheck.backend.repository.ProgressRecordRepository;
import com.redcheck.backend.repository.TaskRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProgressRecordService {

    private final ProgressRecordRepository progressRecordRepository;
    private final TaskRepository taskRepository;

    // Heatmap of the last year (by default)
    public List<ProgressRecordResponseDTO> getHeatmap(User currentUser){
        LocalDate from = LocalDate.now().minusYears(1);
        LocalDate to = LocalDate.now();

        return progressRecordRepository
                .findAllByUserAndDateBetweenOrderByDateAsc(currentUser, from, to)
                .stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    public ProgressRecordResponseDTO getDayProgress(User currentUser, LocalDate date){
        return progressRecordRepository
                .findByUserAndDate(currentUser, date)
                .map(this::toResponseDTO)
                .orElse(emptyRecord(date));
    }

    @Transactional
    public void generateDailyRecord(User user){
        LocalDate today = LocalDate.now();

        if(progressRecordRepository.existsByUserAndDate(user, today)) return;

        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);

        long completedToday = taskRepository.countBySubjectUserIdAndCompletedDateBetween(
                user.getId(), startOfDay, endOfDay);

        long pendingRightNow = taskRepository.countBySubjectUserIdAndCompletedDateIsNull(user.getId());

        long totalForToday = completedToday + pendingRightNow;

        ProgressRecord progressRecord = ProgressRecord.builder()
                .totalTasks((int) totalForToday)
                .completedTasks((int) completedToday)
                .date(today)
                .user(user)
                .build();

        progressRecordRepository.save(progressRecord);
    }

    // --- Auxiliary methods ---

    private ProgressRecordResponseDTO toResponseDTO(ProgressRecord progressRecord){
        double rate = progressRecord.getTotalTasks() == 0 ? 0.0
                : (double) progressRecord.getCompletedTasks()/ progressRecord.getTotalTasks();

        return ProgressRecordResponseDTO.builder()
                .date(progressRecord.getDate())
                .totalTasks(progressRecord.getTotalTasks())
                .completedTasks(progressRecord.getCompletedTasks())
                .completionRate(Math.round(rate * 10.0)/ 10.0)
                .build();
    }

    private ProgressRecordResponseDTO emptyRecord(LocalDate date){
        return ProgressRecordResponseDTO.builder()
                .date(date)
                .totalTasks(0)
                .completedTasks(0)
                .completionRate(0.0)
                .build();
    }
}

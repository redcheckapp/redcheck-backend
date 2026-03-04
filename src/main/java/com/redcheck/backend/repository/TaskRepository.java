package com.redcheck.backend.repository;

import com.redcheck.backend.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findAllBySubject_User_Id(Long userId);
    List<Task> findAllBySubject_User_IdAndCompletedDateIsNull(Long userId);
    List<Task> findAllBySubject_User_IdAndCompletedDateIsNotNull(Long userId);
    List<Task> findAllBySubject_User_IdAndDeadlineBeforeAndCompletedDateIsNull(Long userId, LocalDateTime now);

    Optional<Task> findById(Long id);
}

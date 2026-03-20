package com.redcheck.backend.repository;

import com.redcheck.backend.entity.RecurringTask;
import com.redcheck.backend.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findAllBySubject_User_Id(Long userId);
    List<Task> findAllBySubject_User_IdAndCompletedDateIsNull(Long userId);
    List<Task> findAllBySubject_User_IdAndCompletedDateIsNotNull(Long userId);
    List<Task> findAllBySubject_User_IdAndDeadlineBeforeAndCompletedDateIsNull(Long userId, LocalDateTime now);
    Optional<Task> findTopByRecurringTaskOrderByAssignedDateDesc(RecurringTask recurringTask);
    Optional<Task> findById(Long id);

    @Modifying
    @Query("UPDATE Task t SET t.recurringTask = null WHERE t.recurringTask = :recurringTask")
    void detachFromRecurringTask(@Param("recurringTask") RecurringTask recurringTask);
}

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

    @Query("SELECT t FROM Task t WHERE t.subject.user.id = :userId AND (t.completedDate IS NULL OR (t.completedDate >= :startOfDay AND t.completedDate <= :endOfDay))")
    List<Task> findPendingOrCompletedToday(
            @Param("userId") Long userId,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay
    );

    Optional<Task> findTopByRecurringTaskOrderByAssignedDateDesc(RecurringTask recurringTask);
    Optional<Task> findById(Long id);

    @Modifying
    @Query("UPDATE Task t SET t.recurringTask = null WHERE t.recurringTask = :recurringTask")
    void detachFromRecurringTask(@Param("recurringTask") RecurringTask recurringTask);

    long countBySubjectUserId(Long userId);
    long countBySubjectUserIdAndCompletedDateIsNotNull(Long userId);

    long countBySubjectUserIdAndCompletedDateBetween(Long userId, LocalDateTime startOfDay, LocalDateTime endOfDay);
    long countBySubjectUserIdAndCompletedDateIsNull(Long userId);
}

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

    List<Task> findAllBySubject_User_IdAndDeletedFalse(Long userId);
    List<Task> findAllBySubject_User_IdAndCompletedDateIsNullAndDeletedFalse(Long userId);
    List<Task> findAllBySubject_User_IdAndCompletedDateIsNotNullAndDeletedFalse(Long userId);
    List<Task> findAllBySubject_User_IdAndDeadlineBeforeAndCompletedDateIsNullAndDeletedFalse(Long userId, LocalDateTime now);

    @Query("SELECT t FROM Task t WHERE t.subject.user.id = :userId AND t.deleted = false AND (t.completedDate IS NULL OR (t.completedDate >= :startOfDay AND t.completedDate <= :endOfDay))")
    List<Task> findPendingOrCompletedTodayAndDeletedFalse(
            @Param("userId") Long userId,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay
    );

    Optional<Task> findTopByRecurringTaskAndDeletedFalseOrderByAssignedDateDesc(RecurringTask recurringTask);

    @Modifying
    @Query("UPDATE Task t SET t.recurringTask = null WHERE t.recurringTask = :recurringTask")
    void detachFromRecurringTask(@Param("recurringTask") RecurringTask recurringTask);

    long countBySubjectUserIdAndCompletedDateBetweenAndDeletedFalse(Long userId, LocalDateTime startOfDay, LocalDateTime endOfDay);
    long countBySubjectUserIdAndCompletedDateIsNullAndDeletedFalse(Long userId);

    List<Task> findAllBySubject_User_IdAndDeletedTrue(Long userId);

    @Modifying
    @Query("DELETE FROM Task t WHERE t.deleted = true AND t.deletedAt < :cutoffDate")
    void deleteTrashOlderThan(LocalDateTime cutoffDate);

    @Query("SELECT t.subject.name, " +
            "(COUNT(CASE WHEN t.completedDate IS NOT NULL THEN 1 END) * 100) / COUNT(t) " +
            "FROM Task t WHERE t.subject.user.id = :userId AND t.deleted = false " +
            "GROUP BY t.subject.name")
    List<Object[]> getSubjectCompletionRatios(@Param("userId") Long userId);
}
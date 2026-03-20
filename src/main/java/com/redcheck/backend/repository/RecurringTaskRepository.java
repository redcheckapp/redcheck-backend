package com.redcheck.backend.repository;

import com.redcheck.backend.entity.RecurringTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecurringTaskRepository extends JpaRepository<RecurringTask, Long> {

    List<RecurringTask> findAllBySubject_User_IdAndActive(Long userId, Boolean active);
    List<RecurringTask> findAllBySubject_User_Id(Long userId);
    List<RecurringTask> findAllByActiveTrue();
}

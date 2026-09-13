package com.redcheck.backend.repository;

import com.redcheck.backend.entity.EventCategory;
import com.redcheck.backend.entity.RecurringCalendarEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RecurringCalendarEventRepository extends JpaRepository<RecurringCalendarEvent, Long> {

    List<RecurringCalendarEvent> findAllByUser_Id(Long userId);

    List<RecurringCalendarEvent> findAllByUser_IdAndActive(Long userId, boolean active);

    List<RecurringCalendarEvent> findAllByActiveTrue();

    @Modifying
    @Query("UPDATE RecurringCalendarEvent r SET r.category = null WHERE r.category = :category")
    void detachFromCategory(@Param("category") EventCategory category);
}

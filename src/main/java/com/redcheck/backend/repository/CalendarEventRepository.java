package com.redcheck.backend.repository;

import com.redcheck.backend.entity.CalendarEvent;
import com.redcheck.backend.entity.EventCategory;
import com.redcheck.backend.entity.RecurringCalendarEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface CalendarEventRepository extends JpaRepository<CalendarEvent, Long> {

    // Classic interval-overlap check: any event whose [start, end] overlaps
    // the requested [from, to] window — every row's endDateTime is always
    // set (see CalendarEvent#endDateTime), so this covers punctual,
    // all-day and multi-day events alike with one query.
    @Query("SELECT e FROM CalendarEvent e WHERE e.user.id = :userId AND e.startDateTime <= :to AND e.endDateTime >= :from")
    List<CalendarEvent> findAllByUser_IdAndDateRange(
            @Param("userId") Long userId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    @Modifying
    @Query("UPDATE CalendarEvent e SET e.recurringEvent = null WHERE e.recurringEvent = :recurringEvent")
    void detachFromRecurringEvent(@Param("recurringEvent") RecurringCalendarEvent recurringEvent);

    @Modifying
    @Query("UPDATE CalendarEvent e SET e.category = null WHERE e.category = :category")
    void detachFromCategory(@Param("category") EventCategory category);
}

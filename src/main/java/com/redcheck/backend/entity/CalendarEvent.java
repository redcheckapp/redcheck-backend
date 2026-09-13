package com.redcheck.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalendarEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    private String description;

    @Column(name = "start_date_time", nullable = false)
    private LocalDateTime startDateTime;

    // Always set, even for a punctual event (see CalendarEventService,
    // which defaults it to startDateTime when the request omits it) — every
    // row is a well-formed interval, unlike Task.deadline which is
    // genuinely optional/absent. This is what actually distinguishes an
    // event from a Task: a Task is a single point in time, an event always
    // has a start/end interval (zero-length for a punctual one).
    @Column(name = "end_date_time", nullable = false)
    private LocalDateTime endDateTime;

    @Builder.Default
    @Column(name = "all_day", nullable = false)
    private boolean allDay = false;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private EventCategory category;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recurring_event_id")
    private RecurringCalendarEvent recurringEvent;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}

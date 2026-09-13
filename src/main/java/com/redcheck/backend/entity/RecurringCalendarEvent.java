package com.redcheck.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecurringCalendarEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    private String description;

    @Builder.Default
    @Column(name = "all_day", nullable = false)
    private boolean allDay = false;

    // Time-of-day applied to every generated occurrence's startDateTime.
    // Ignored (and irrelevant) when allDay — mirrors RecurringTask#time.
    @Column(name = "generation_time")
    private LocalTime time;

    // Duration (in minutes) applied on top of the generation time to
    // compute each occurrence's endDateTime. Ignored when allDay, since an
    // all-day occurrence just spans that calendar day.
    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(nullable = false)
    private String frequency;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @Column(name = "latest_generated_date")
    private LocalDateTime latestGeneratedDate;

    // Optional last day this routine should still generate occurrences on —
    // same auto-deactivation behavior as RecurringTask#endDate.
    @Column(name = "end_date")
    private LocalDate endDate;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private EventCategory category;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Builder.Default
    @OneToMany(mappedBy = "recurringEvent")
    private List<CalendarEvent> generatedEvents = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdDate = LocalDateTime.now();
    }
}

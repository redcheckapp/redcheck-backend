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
public class RecurringTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    private String description;

    @Column(nullable = false)
    private String frequency;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @Column(name = "latest_generated_date")
    private LocalDateTime latestGeneratedDate;

    // Optional time-of-day applied to every generated Task's deadline (see
    // RecurringTaskSchedulerService#generateTask) — without it, generated
    // tasks have no deadline at all, same as before this field existed.
    @Column(name = "generation_time")
    private LocalTime time;

    // Optional last day this routine should still generate tasks on —
    // RecurringTaskSchedulerService auto-deactivates the routine once this
    // date has passed rather than requiring the user to remember to pause
    // or delete it themselves.
    @Column(name = "end_date")
    private LocalDate endDate;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Builder.Default
    @OneToMany(mappedBy = "recurringTask")
    private List<Task> generatedTasks = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdDate = LocalDateTime.now();
    }
}
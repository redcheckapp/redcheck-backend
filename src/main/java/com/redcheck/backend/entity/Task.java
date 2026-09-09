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
public class Task {

    public enum Priority {
        LOW, MEDIUM, HIGH
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    private String description;

    @Column(name = "assigned_date", nullable = false, updatable = false)
    private LocalDateTime assignedDate;

    private LocalDateTime deadline;

    @Column(name = "completed_date")
    private LocalDateTime completedDate;

    // Applied via Hibernate's ddl-auto=update (this project's only schema
    // migration mechanism, no Flyway/Liquibase — see backend CLAUDE.md).
    // The DB-level DEFAULT backfills existing rows on that ALTER TABLE and
    // covers any raw INSERT (e.g. init-demo.sql) that doesn't set it.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(20) DEFAULT 'MEDIUM'")
    @Builder.Default
    private Priority priority = Priority.MEDIUM;

    @Column(nullable = false)
    private boolean deleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recurring_task_id")
    private RecurringTask recurringTask;

    @PrePersist
    protected void onCreate() {
        assignedDate = LocalDateTime.now();
    }
}
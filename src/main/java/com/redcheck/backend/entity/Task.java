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

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @PrePersist
    protected void onCreate() {
        assignedDate = LocalDateTime.now();
    }
}

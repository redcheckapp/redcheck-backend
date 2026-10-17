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
public class Notification {

    // SYSTEM covers every notification created before types existed (the
    // SmartCheck AI ones). The frontend's bell only lists FEEDBACK_REPLY for
    // now — SYSTEM rows are English-only and already surfaced by the AI
    // modal's own "analysis ready" UI, so showing them too would duplicate it.
    public enum Type {
        SYSTEM,
        FEEDBACK_REPLY
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String message;

    // VARCHAR + DEFAULT so ddl-auto=update can add it to the existing rows.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(30) DEFAULT 'SYSTEM'")
    @Builder.Default
    private Type type = Type.SYSTEM;

    @Builder.Default
    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    @Column(name = "creation_date", nullable = false, updatable = false)
    private LocalDateTime creationDate;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @PrePersist
    protected void onCreate() {
        creationDate = LocalDateTime.now();
    }
}
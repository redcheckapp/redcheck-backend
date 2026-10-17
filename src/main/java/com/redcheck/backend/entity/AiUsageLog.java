package com.redcheck.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

// One row per outbound call to the SmartCheck AI engine, success or failure,
// for the developer dashboard's cost/limits view. Separate from AiResponse on
// purpose: AiResponse only keeps the latest successful analysis (re-running
// one deletes today's previous row, see SmartCheckAIController#dailySmartAnalysis)
// and never records failures, so counting AiResponse rows would undercount.
// Each PRIORITIZE call costs one Gemini generate_content (up to 3 with the
// engine's own retries) plus one embedding; each TASK_OUTCOME call costs one
// embedding.
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "user")
@EqualsAndHashCode(exclude = "user")
public class AiUsageLog {

    public enum Type {
        PRIORITIZE,
        TASK_OUTCOME
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(20)")
    private Type type;

    @Column(nullable = false)
    private boolean success;

    @Column(name = "duration_ms", nullable = false)
    private long durationMs;

    // Size of the request sent to the engine (pending tasks for PRIORITIZE,
    // always 1 for TASK_OUTCOME) — a rough proxy for prompt size/token cost.
    @Column(name = "task_count", nullable = false)
    private int taskCount;

    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate;

    // Same keep-the-row-drop-the-link pattern as Feedback.user: usage history
    // should survive account deletion (it's what was actually spent), and a
    // leftover log row must never block deleting a user.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private User user;

    @PrePersist
    protected void onCreate() {
        createdDate = LocalDateTime.now();
    }
}

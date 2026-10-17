package com.redcheck.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "user")
@EqualsAndHashCode(exclude = "user")
public class Feedback {

    public enum Category {
        BUG, SUGGESTION, PRAISE, OTHER
    }

    // Triage state, only ever changed from the developer dashboard
    // (PATCH /admin/feedback/{id}). SEEN is "read, not acted on yet" —
    // distinct from NEW so the inbox's unread count means something.
    public enum Status {
        NEW, SEEN, IN_PROGRESS, DONE, WONTFIX
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String message;

    // VARCHAR + DEFAULT (not Hibernate's native MySQL ENUM) so ddl-auto=update
    // can add it to a table that already has rows — same as Task.priority.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(20) DEFAULT 'NEW'")
    @Builder.Default
    private Status status = Status.NEW;

    // Private note for the developer dashboard; never shown to the user.
    @Column(name = "admin_note", columnDefinition = "TEXT")
    private String adminNote;

    // Last time the author was sent an in-app reply from the dashboard
    // (POST /admin/feedback/{id}/notify). Null = never.
    @Column(name = "user_notified_date")
    private LocalDateTime userNotifiedDate;

    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate;

    // Deliberately NOT cascaded from User (unlike Subject/Task/Notification/
    // AiResponse/ProgressRecord) and NOT deleted when the user is: feedback
    // is product data the team wants to keep even after account deletion.
    // ON DELETE SET NULL at the DB level means deleting a user never fails
    // on a leftover feedback row — it just orphans it.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private User user;

    @PrePersist
    protected void onCreate() {
        createdDate = LocalDateTime.now();
    }
}

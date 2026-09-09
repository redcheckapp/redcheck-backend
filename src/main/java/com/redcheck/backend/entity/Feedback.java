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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String message;

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

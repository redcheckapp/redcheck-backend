package com.redcheck.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "creation_date", nullable = false, updatable = false)
    private LocalDateTime creationDate;

    //This method executes automatically before saving user for the first time
    @PrePersist
    protected void onCreate(){
        creationDate = LocalDateTime.now();
    }
}

package com.redcheck.backend.repository;

import com.redcheck.backend.entity.Notification;
import com.redcheck.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findAllByUserAndRead(User user, boolean read);

    List<Notification> findAllByUser(User user);

    List<Notification> findAllByUserAndReadAndTypeOrderByCreationDateDesc(User user, boolean read, Notification.Type type);

    List<Notification> findAllByUserAndTypeOrderByCreationDateDesc(User user, Notification.Type type);

    Optional<Notification> findByIdAndUser(Long id, User user);
}
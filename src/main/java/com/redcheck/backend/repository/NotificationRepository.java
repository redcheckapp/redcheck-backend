package com.redcheck.backend.repository;

import com.redcheck.backend.entity.Notification;
import com.redcheck.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findAllByUserAndRead(User user, boolean read);

    List<Notification> findAllByUser(User user);
}
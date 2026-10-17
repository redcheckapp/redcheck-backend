package com.redcheck.backend.service;

import com.redcheck.backend.dto.response.NotificationResponseDTO;
import com.redcheck.backend.entity.Notification;
import com.redcheck.backend.entity.User;
import com.redcheck.backend.exception.NotificationNotFoundException;
import com.redcheck.backend.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public List<NotificationResponseDTO> getNotifications(@AuthenticationPrincipal User currentUser, Boolean read) {

        List<Notification> rawNotifications;

        if (read != null) {
            rawNotifications = notificationRepository.findAllByUserAndRead(currentUser, read);
        } else {
            rawNotifications = notificationRepository.findAllByUser(currentUser);
        }

        return rawNotifications
                .stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    // Same as getNotifications, narrowed to one type, newest first.
    public List<NotificationResponseDTO> getNotificationsByType(User currentUser, Boolean read, Notification.Type type) {
        List<Notification> rawNotifications = read != null
                ? notificationRepository.findAllByUserAndReadAndTypeOrderByCreationDateDesc(currentUser, read, type)
                : notificationRepository.findAllByUserAndTypeOrderByCreationDateDesc(currentUser, type);

        return rawNotifications.stream().map(this::toResponseDTO).toList();
    }

    // Looked up by id AND owner, so someone else's notification is
    // indistinguishable from a missing one.
    @Transactional
    public void markAsRead(User currentUser, Long id) {
        Notification notification = notificationRepository.findByIdAndUser(id, currentUser)
                .orElseThrow(() -> new NotificationNotFoundException(id));
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    // --- Auxiliary methods ---

    private NotificationResponseDTO toResponseDTO(Notification notification) {
        return NotificationResponseDTO.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .type(notification.getType())
                .read(notification.isRead())
                .creationDate(notification.getCreationDate())
                .build();
    }
}
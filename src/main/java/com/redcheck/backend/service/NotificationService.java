package com.redcheck.backend.service;

import com.redcheck.backend.dto.response.NotificationResponseDTO;
import com.redcheck.backend.entity.Notification;
import com.redcheck.backend.entity.User;
import com.redcheck.backend.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public List<NotificationResponseDTO> getNotifications(@AuthenticationPrincipal User currentUser, Boolean read){

        List<Notification> rawNotifications;

        if(read != null)
            rawNotifications = notificationRepository.findAllByUserAndRead(currentUser, read);
        else
            rawNotifications = notificationRepository.findAllByUser(currentUser);

        return rawNotifications
                .stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    // --- Auxiliary methods ---

    private NotificationResponseDTO toResponseDTO(Notification notification){
        return NotificationResponseDTO.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .read(notification.isRead())
                .creationDate(notification.getCreationDate())
                .build();
    }
}

package com.redcheck.backend.dto.response;

import com.redcheck.backend.entity.Notification;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record NotificationResponseDTO(
        Long id,
        String title,
        String message,
        Notification.Type type,
        boolean read,
        LocalDateTime creationDate
) {}
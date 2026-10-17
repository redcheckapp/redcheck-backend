package com.redcheck.backend.controller;

import com.redcheck.backend.dto.response.NotificationResponseDTO;
import com.redcheck.backend.entity.Notification;
import com.redcheck.backend.entity.User;
import com.redcheck.backend.exception.NotificationNotFoundException;
import com.redcheck.backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@CrossOrigin(origins = "${app.cors.allowed-origins}")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<NotificationResponseDTO>> getNotifications(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(required = false) Boolean read,
            @RequestParam(required = false) Notification.Type type) {

        List<NotificationResponseDTO> response = type != null
                ? notificationService.getNotificationsByType(currentUser, read, type)
                : notificationService.getNotifications(currentUser, read);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@AuthenticationPrincipal User currentUser, @PathVariable Long id) {
        try {
            notificationService.markAsRead(currentUser, id);
            return ResponseEntity.noContent().build();
        } catch (NotificationNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
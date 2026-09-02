package com.redcheck.backend.controller;

import com.redcheck.backend.dto.response.NotificationResponseDTO;
import com.redcheck.backend.entity.User;
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
            @RequestParam(required = false) Boolean read) {

        List<NotificationResponseDTO> response = notificationService.getNotifications(currentUser, read);
        return ResponseEntity.ok(response);
    }
}
package com.redcheck.backend.controller;

import com.redcheck.backend.dto.request.FeedbackReplyRequestDTO;
import com.redcheck.backend.dto.response.AdminFeedbackResponseDTO;
import com.redcheck.backend.dto.response.AiUsageStatsResponseDTO;
import com.redcheck.backend.dto.response.SystemStatusResponseDTO;
import com.redcheck.backend.dto.response.UserStatsResponseDTO;
import com.redcheck.backend.dto.update.AdminFeedbackUpdateDTO;
import com.redcheck.backend.exception.FeedbackAuthorDeletedException;
import com.redcheck.backend.exception.FeedbackNotFoundException;
import com.redcheck.backend.service.AiUsageService;
import com.redcheck.backend.service.FeedbackService;
import com.redcheck.backend.service.SystemStatusService;
import com.redcheck.backend.service.UserStatsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Developer dashboard API. Access is enforced in SecurityConfig (/admin/**
// is limited to ADMIN_EMAILS), not here — nothing in this class should be
// reachable by a regular user.
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@CrossOrigin(origins = "${app.cors.allowed-origins}")
public class AdminController {

    private static final int MAX_USAGE_DAYS = 90;

    private final FeedbackService feedbackService;
    private final AiUsageService aiUsageService;
    private final UserStatsService userStatsService;
    private final SystemStatusService systemStatusService;

    @GetMapping("/feedback")
    public ResponseEntity<List<AdminFeedbackResponseDTO>> getAllFeedback() {
        return ResponseEntity.ok(feedbackService.getAllFeedbackForAdmin());
    }

    @PatchMapping("/feedback/{id}")
    public ResponseEntity<AdminFeedbackResponseDTO> updateFeedback(
            @PathVariable Long id,
            @Valid @RequestBody AdminFeedbackUpdateDTO updateDTO) {
        try {
            return ResponseEntity.ok(feedbackService.updateFeedbackForAdmin(id, updateDTO));
        } catch (FeedbackNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/feedback/{id}/notify")
    public ResponseEntity<AdminFeedbackResponseDTO> notifyAuthor(
            @PathVariable Long id,
            @Valid @RequestBody FeedbackReplyRequestDTO requestDTO) {
        try {
            return ResponseEntity.ok(feedbackService.notifyAuthor(id, requestDTO));
        } catch (FeedbackNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (FeedbackAuthorDeletedException e) {
            // 409: the feedback exists, but there's nobody left to notify.
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @GetMapping("/ai-usage")
    public ResponseEntity<AiUsageStatsResponseDTO> getAiUsage(@RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(aiUsageService.getStats(clampDays(days)));
    }

    @GetMapping("/user-stats")
    public ResponseEntity<UserStatsResponseDTO> getUserStats(@RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(userStatsService.getStats(clampDays(days)));
    }

    @GetMapping("/system")
    public ResponseEntity<SystemStatusResponseDTO> getSystemStatus() {
        return ResponseEntity.ok(systemStatusService.getStatus());
    }

    private static int clampDays(int days) {
        return Math.max(1, Math.min(days, MAX_USAGE_DAYS));
    }
}

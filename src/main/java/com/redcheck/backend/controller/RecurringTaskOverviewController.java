package com.redcheck.backend.controller;

import com.redcheck.backend.dto.response.RecurringTaskResponseDTO;
import com.redcheck.backend.entity.User;
import com.redcheck.backend.service.RecurringTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Top-level (not nested under /subjects/{id}) so the global "all my
// routines" view can fetch every subject's recurring tasks in one request
// instead of one request per subject — same N+1-avoidance reasoning as
// SubjectController#getAllWithTasks and TaskCalendarController. Reuses
// RecurringTaskService#getAllRecurringTask (already subject-optional, the
// nested RecurringTaskController just never exercised that path before).
@RestController
@RequestMapping("/recurring-tasks")
@RequiredArgsConstructor
@CrossOrigin(origins = "${app.cors.allowed-origins}")
public class RecurringTaskOverviewController {

    private final RecurringTaskService recurringTaskService;

    @GetMapping
    public ResponseEntity<List<RecurringTaskResponseDTO>> getAll(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(required = false) Boolean active) {

        List<RecurringTaskResponseDTO> response = recurringTaskService.getAllRecurringTask(currentUser, null, active);
        return ResponseEntity.ok(response);
    }
}

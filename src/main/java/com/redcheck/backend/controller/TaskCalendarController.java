package com.redcheck.backend.controller;

import com.redcheck.backend.dto.response.TaskResponseDTO;
import com.redcheck.backend.entity.User;
import com.redcheck.backend.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

// Top-level (not nested under /subjects/{id}) so the calendar can fetch
// every subject's tasks for a date range in one request instead of one
// request per subject — same N+1-avoidance reasoning as
// SubjectController#getAllWithTasks.
@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
@CrossOrigin(origins = "${app.cors.allowed-origins}")
public class TaskCalendarController {

    private final TaskService taskService;

    @GetMapping
    public ResponseEntity<List<TaskResponseDTO>> getByDateRange(
            @AuthenticationPrincipal User currentUser,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        List<TaskResponseDTO> response = taskService.getTasksForDateRange(currentUser, from, to);
        return ResponseEntity.ok(response);
    }
}

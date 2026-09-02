package com.redcheck.backend.controller;

import com.redcheck.backend.dto.update.TaskCompleteDTO;
import com.redcheck.backend.dto.request.TaskRequestDTO;
import com.redcheck.backend.dto.response.TaskResponseDTO;
import com.redcheck.backend.entity.User;
import com.redcheck.backend.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/subjects/{subjectId}/tasks")
@RequiredArgsConstructor
@CrossOrigin(origins = "${app.cors.allowed-origins}")
public class TaskController {

    private final TaskService taskService;

    @GetMapping
    public ResponseEntity<List<TaskResponseDTO>> getAll(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long subjectId,
            @RequestParam(required = false) Boolean completed,
            @RequestParam(required = false) Boolean overdue,
            @RequestParam(required = false) Boolean deleted) {

        List<TaskResponseDTO> response = taskService.getAllTask(currentUser, subjectId, completed, overdue, deleted);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<TaskResponseDTO> create(
            @PathVariable Long subjectId,
            @Valid @RequestBody TaskRequestDTO requestDTO,
            @AuthenticationPrincipal User currentUser) {

        TaskResponseDTO response = taskService.createTask(subjectId, requestDTO, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{taskId}")
    public ResponseEntity<TaskResponseDTO> update(
            @PathVariable Long subjectId,
            @PathVariable Long taskId,
            @Valid @RequestBody TaskRequestDTO requestDTO,
            @AuthenticationPrincipal User currentUser) {

        TaskResponseDTO response = taskService.updateTask(subjectId, taskId, requestDTO, currentUser);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{taskId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long subjectId,
            @PathVariable Long taskId,
            @AuthenticationPrincipal User currentUser) {

        taskService.deleteTask(subjectId, taskId, currentUser);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{taskId}/force")
    public ResponseEntity<Void> hardDelete(
            @PathVariable Long subjectId,
            @PathVariable Long taskId,
            @AuthenticationPrincipal User currentUser) {

        taskService.hardDeleteTask(subjectId, taskId, currentUser);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{taskId}/restore")
    public ResponseEntity<TaskResponseDTO> restore(
            @PathVariable Long subjectId,
            @PathVariable Long taskId,
            @AuthenticationPrincipal User currentUser) {

        TaskResponseDTO responseDTO = taskService.restoreTask(subjectId, taskId, currentUser);
        return ResponseEntity.ok(responseDTO);
    }

    @PatchMapping("/{taskId}/complete")
    public ResponseEntity<TaskResponseDTO> complete(
            @PathVariable Long subjectId,
            @PathVariable Long taskId,
            @Valid @RequestBody TaskCompleteDTO requestDTO,
            @AuthenticationPrincipal User currentUser) {

        TaskResponseDTO response = taskService.markTaskAsCompleted(subjectId, taskId, requestDTO, currentUser);
        return ResponseEntity.ok(response);
    }
}
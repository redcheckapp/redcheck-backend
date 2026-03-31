package com.redcheck.backend.controller;

import com.redcheck.backend.dto.request.RecurringTaskRequestDTO;
import com.redcheck.backend.dto.response.RecurringTaskResponseDTO;
import com.redcheck.backend.dto.update.RecurringTaskActiveDTO;
import com.redcheck.backend.entity.User;
import com.redcheck.backend.service.RecurringTaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/subjects/{subjectId}/recurring-tasks")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RecurringTaskController {

    private final RecurringTaskService recurringTaskService;

    @GetMapping
    public ResponseEntity<List<RecurringTaskResponseDTO>> getAll(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long subjectId,
            @RequestParam(required = false) Boolean active){

        List<RecurringTaskResponseDTO> response = recurringTaskService.getAllRecurringTask(currentUser, subjectId, active);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<RecurringTaskResponseDTO> create(
            @PathVariable Long subjectId,
            @Valid @RequestBody RecurringTaskRequestDTO requestDTO,
            @AuthenticationPrincipal User currentUser){

        RecurringTaskResponseDTO response = recurringTaskService.createRecurringTask(subjectId, requestDTO, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{recurringTaskId}")
    public ResponseEntity<RecurringTaskResponseDTO> update(
            @PathVariable Long subjectId,
            @PathVariable Long recurringTaskId,
            @Valid @RequestBody RecurringTaskRequestDTO requestDTO,
            @AuthenticationPrincipal User currentUser){

        RecurringTaskResponseDTO response = recurringTaskService.updateRecurringTask(subjectId, recurringTaskId, requestDTO, currentUser);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{recurringTaskId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long subjectId,
            @PathVariable Long recurringTaskId,
            @AuthenticationPrincipal User currentUser){

        recurringTaskService.deleteRecurringTask(subjectId, recurringTaskId, currentUser);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{recurringTaskId}/active")
    public ResponseEntity<RecurringTaskResponseDTO> active(
            @PathVariable Long subjectId,
            @PathVariable Long recurringTaskId,
            @Valid @RequestBody RecurringTaskActiveDTO requestDTO,
            @AuthenticationPrincipal User currentUser){

        RecurringTaskResponseDTO response = recurringTaskService.activateRecurringTask(subjectId, recurringTaskId, requestDTO, currentUser);
        return ResponseEntity.ok(response);
    }
}

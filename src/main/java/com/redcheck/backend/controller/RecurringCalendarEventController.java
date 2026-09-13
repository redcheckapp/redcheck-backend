package com.redcheck.backend.controller;

import com.redcheck.backend.dto.request.RecurringCalendarEventRequestDTO;
import com.redcheck.backend.dto.response.RecurringCalendarEventResponseDTO;
import com.redcheck.backend.dto.update.RecurringCalendarEventActiveDTO;
import com.redcheck.backend.entity.User;
import com.redcheck.backend.service.RecurringCalendarEventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/recurring-calendar-events")
@RequiredArgsConstructor
@CrossOrigin(origins = "${app.cors.allowed-origins}")
public class RecurringCalendarEventController {

    private final RecurringCalendarEventService recurringCalendarEventService;

    @GetMapping
    public ResponseEntity<List<RecurringCalendarEventResponseDTO>> getAll(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(required = false) Boolean active) {

        return ResponseEntity.ok(recurringCalendarEventService.getAllRecurringEvents(currentUser, active));
    }

    @PostMapping
    public ResponseEntity<RecurringCalendarEventResponseDTO> create(
            @Valid @RequestBody RecurringCalendarEventRequestDTO requestDTO,
            @AuthenticationPrincipal User currentUser) {

        RecurringCalendarEventResponseDTO response = recurringCalendarEventService.createRecurringEvent(requestDTO, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{recurringEventId}")
    public ResponseEntity<RecurringCalendarEventResponseDTO> update(
            @PathVariable Long recurringEventId,
            @Valid @RequestBody RecurringCalendarEventRequestDTO requestDTO,
            @AuthenticationPrincipal User currentUser) {

        RecurringCalendarEventResponseDTO response = recurringCalendarEventService.updateRecurringEvent(recurringEventId, requestDTO, currentUser);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{recurringEventId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long recurringEventId,
            @AuthenticationPrincipal User currentUser) {

        recurringCalendarEventService.deleteRecurringEvent(recurringEventId, currentUser);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{recurringEventId}/active")
    public ResponseEntity<RecurringCalendarEventResponseDTO> active(
            @PathVariable Long recurringEventId,
            @Valid @RequestBody RecurringCalendarEventActiveDTO requestDTO,
            @AuthenticationPrincipal User currentUser) {

        RecurringCalendarEventResponseDTO response = recurringCalendarEventService.setActive(recurringEventId, requestDTO.active(), currentUser);
        return ResponseEntity.ok(response);
    }
}

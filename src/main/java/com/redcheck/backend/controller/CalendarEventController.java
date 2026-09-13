package com.redcheck.backend.controller;

import com.redcheck.backend.dto.request.CalendarEventRequestDTO;
import com.redcheck.backend.dto.response.CalendarEventResponseDTO;
import com.redcheck.backend.entity.User;
import com.redcheck.backend.service.CalendarEventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

// Top-level, own domain — a CalendarEvent is not a Task and isn't nested
// under /subjects/{id}; it belongs directly to the user (see CalendarEvent
// entity). GET here mirrors TaskCalendarController's ?from=&to= contract so
// the frontend can fetch one range per visible calendar view.
@RestController
@RequestMapping("/calendar-events")
@RequiredArgsConstructor
@CrossOrigin(origins = "${app.cors.allowed-origins}")
public class CalendarEventController {

    private final CalendarEventService calendarEventService;

    @GetMapping
    public ResponseEntity<List<CalendarEventResponseDTO>> getByDateRange(
            @AuthenticationPrincipal User currentUser,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        return ResponseEntity.ok(calendarEventService.getEventsForDateRange(currentUser, from, to));
    }

    @PostMapping
    public ResponseEntity<CalendarEventResponseDTO> create(
            @Valid @RequestBody CalendarEventRequestDTO requestDTO,
            @AuthenticationPrincipal User currentUser) {

        CalendarEventResponseDTO response = calendarEventService.createEvent(requestDTO, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{eventId}")
    public ResponseEntity<CalendarEventResponseDTO> update(
            @PathVariable Long eventId,
            @Valid @RequestBody CalendarEventRequestDTO requestDTO,
            @AuthenticationPrincipal User currentUser) {

        CalendarEventResponseDTO response = calendarEventService.updateEvent(eventId, requestDTO, currentUser);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{eventId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long eventId,
            @AuthenticationPrincipal User currentUser) {

        calendarEventService.deleteEvent(eventId, currentUser);
        return ResponseEntity.noContent().build();
    }
}

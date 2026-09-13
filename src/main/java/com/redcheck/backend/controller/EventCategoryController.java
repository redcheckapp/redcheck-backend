package com.redcheck.backend.controller;

import com.redcheck.backend.dto.request.EventCategoryRequestDTO;
import com.redcheck.backend.dto.response.EventCategoryResponseDTO;
import com.redcheck.backend.entity.User;
import com.redcheck.backend.service.EventCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/event-categories")
@RequiredArgsConstructor
@CrossOrigin(origins = "${app.cors.allowed-origins}")
public class EventCategoryController {

    private final EventCategoryService eventCategoryService;

    @GetMapping
    public ResponseEntity<List<EventCategoryResponseDTO>> getAll(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(eventCategoryService.getAllCategories(currentUser));
    }

    @PostMapping
    public ResponseEntity<EventCategoryResponseDTO> create(
            @Valid @RequestBody EventCategoryRequestDTO requestDTO,
            @AuthenticationPrincipal User currentUser) {

        EventCategoryResponseDTO response = eventCategoryService.createCategory(requestDTO, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{categoryId}")
    public ResponseEntity<EventCategoryResponseDTO> update(
            @PathVariable Long categoryId,
            @Valid @RequestBody EventCategoryRequestDTO requestDTO,
            @AuthenticationPrincipal User currentUser) {

        EventCategoryResponseDTO response = eventCategoryService.updateCategory(categoryId, requestDTO, currentUser);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{categoryId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long categoryId,
            @AuthenticationPrincipal User currentUser) {

        eventCategoryService.deleteCategory(categoryId, currentUser);
        return ResponseEntity.noContent().build();
    }
}

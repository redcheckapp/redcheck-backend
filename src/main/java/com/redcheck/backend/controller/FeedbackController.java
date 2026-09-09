package com.redcheck.backend.controller;

import com.redcheck.backend.dto.request.FeedbackRequestDTO;
import com.redcheck.backend.dto.response.FeedbackResponseDTO;
import com.redcheck.backend.entity.User;
import com.redcheck.backend.service.FeedbackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/feedback")
@RequiredArgsConstructor
@CrossOrigin(origins = "${app.cors.allowed-origins}")
public class FeedbackController {

    private final FeedbackService feedbackService;

    @PostMapping
    public ResponseEntity<FeedbackResponseDTO> create(
            @Valid @RequestBody FeedbackRequestDTO requestDTO,
            @AuthenticationPrincipal User currentUser) {

        FeedbackResponseDTO response = feedbackService.createFeedback(requestDTO, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}

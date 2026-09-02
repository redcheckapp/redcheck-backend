package com.redcheck.backend.controller;

import com.redcheck.backend.dto.response.ProgressRecordResponseDTO;
import com.redcheck.backend.entity.User;
import com.redcheck.backend.service.ProgressRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/progress")
@RequiredArgsConstructor
@CrossOrigin(origins = "${app.cors.allowed-origins}")
public class ProgressRecordController {

    private final ProgressRecordService progressRecordService;

    @GetMapping("/heatmap")
    public ResponseEntity<List<ProgressRecordResponseDTO>> getHeatmap(
            @AuthenticationPrincipal User currentUser) {

        List<ProgressRecordResponseDTO> response = progressRecordService.getHeatmap(currentUser);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/day")
    public ResponseEntity<ProgressRecordResponseDTO> getByDay(
            @AuthenticationPrincipal User currentUser,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        ProgressRecordResponseDTO response = progressRecordService.getDayProgress(currentUser, date);
        return ResponseEntity.ok(response);
    }
}
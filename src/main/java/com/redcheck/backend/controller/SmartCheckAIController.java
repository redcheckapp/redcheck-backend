package com.redcheck.backend.controller;

import com.redcheck.backend.entity.User;
import com.redcheck.backend.service.SmartCheckAIService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
@CrossOrigin(origins = "${app.cors.allowed-origins}")
public class SmartCheckAIController {

    private final SmartCheckAIService smartCheckAIService;

    @GetMapping("/today/analysis")
    public ResponseEntity<String> getTodaysAnalysis(@AuthenticationPrincipal User currentUser) {
        String aiResponseJson = smartCheckAIService.getTodaysAnalysis(currentUser);
        return (aiResponseJson == null || aiResponseJson.isEmpty())
                ? ResponseEntity.notFound().build()
                : ResponseEntity.ok(aiResponseJson);
    }

    @PostMapping("/analyze")
    public ResponseEntity<String> dailySmartAnalysis(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(defaultValue = "es") String lang) {

        smartCheckAIService.deleteTodaysAnalysis(currentUser);
        smartCheckAIService.runDailySmartAnalysis(currentUser, lang);
        return ResponseEntity.ok("Analysis started in the background. We will notify you when it is ready!");
    }
}
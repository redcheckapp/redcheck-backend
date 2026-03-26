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
@CrossOrigin(origins = "*")
public class SmartCheckAIController {

    private final SmartCheckAIService smartCheckAIService;

    @GetMapping("/today/analysis")
    public ResponseEntity<String> getTodaysAnalysis(@AuthenticationPrincipal User currentUser){
        String aiResponseJson = smartCheckAIService.getTodaysAnalysis(currentUser);
        return (aiResponseJson == null || aiResponseJson.isEmpty())
                ? ResponseEntity.notFound().build()
                : ResponseEntity.ok(aiResponseJson);
    }

    @PostMapping("/analyze")
    public ResponseEntity<String> dailySmartAnalysis(@AuthenticationPrincipal User currentUser){
        smartCheckAIService.deleteTodaysAnalysis(currentUser);
        smartCheckAIService.runDailySmartAnalysis(currentUser);
        return ResponseEntity.ok("El análisis ha comenzado en segundo plano. ¡Te notificaremos cuando esté listo!");
    }
}

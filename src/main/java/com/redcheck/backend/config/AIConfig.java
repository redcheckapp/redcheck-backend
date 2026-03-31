package com.redcheck.backend.config;

import com.redcheck.backend.service.AIService;
import com.redcheck.backend.service.GeminiService;
import com.redcheck.backend.service.OllamaService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AIConfig {

    @Value("${ai.provider:gemini}")
    private String provider;

    @Bean
    public AIService aiService(OllamaService ollamaService, GeminiService geminiService) {
        return switch (provider.toLowerCase()) {
            case "gemini" -> geminiService;
            case "ollama" -> ollamaService;
            default -> throw new IllegalArgumentException("AI provider not supported: " + provider);
        };
    }
}
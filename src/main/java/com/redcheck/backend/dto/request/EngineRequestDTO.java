package com.redcheck.backend.dto.request;

import lombok.Builder;

import java.util.List;
import java.util.Map;

@Builder
public record EngineRequestDTO(
        String userId,
        String lang,
        Map<String, Integer> userAnalytics,
        List<Map<String, Object>> tasks
) {}
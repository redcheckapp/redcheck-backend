package com.redcheck.backend.dto.request;

import java.util.List;
import java.util.Map;

public record EngineRequestDTO(
        String userId,
        String userProfile,
        Map<String, Integer> userAnalytics,
        List<Map<String, Object>> tasks
) {}

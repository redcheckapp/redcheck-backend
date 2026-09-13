package com.redcheck.backend.dto.response;

import lombok.Builder;

@Builder
public record EventCategoryResponseDTO(
        Long id,
        String name,
        String color
) {}

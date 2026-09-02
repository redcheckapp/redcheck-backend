package com.redcheck.backend.dto.response;

import lombok.Builder;

@Builder
public record SubjectResponseDTO(
        Long id,
        String name,
        String description,
        boolean deleted,
        boolean archived
) {}
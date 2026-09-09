package com.redcheck.backend.dto.response;

import lombok.Builder;

import java.util.List;

@Builder
public record SubjectWithTasksResponseDTO(
        Long id,
        String name,
        String description,
        boolean deleted,
        boolean archived,
        List<TaskResponseDTO> tasks
) {}

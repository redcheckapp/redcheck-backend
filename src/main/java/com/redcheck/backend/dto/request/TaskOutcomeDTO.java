package com.redcheck.backend.dto.request;

import lombok.Builder;

@Builder
public record TaskOutcomeDTO(
        String userId,
        Long id,
        String titulo,
        String asignatura,
        String fechaLimite,
        boolean completada,
        String fechaCompletado,
        Integer ordenSugeridoIA,
        Integer ordenReal
) {}

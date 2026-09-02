package com.redcheck.backend.dto.update;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record SubjectArchiveDTO(
        @NotNull
        boolean archived
) {}
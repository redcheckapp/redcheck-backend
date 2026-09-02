package com.redcheck.backend.dto.update;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record RecurringTaskActiveDTO(
        @NotNull
        boolean active
) {}
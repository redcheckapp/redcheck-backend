package com.redcheck.backend.dto.update;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecurringTaskActiveDTO {
    @NotNull
    private boolean active;
}

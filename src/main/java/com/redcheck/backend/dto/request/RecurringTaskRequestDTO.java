package com.redcheck.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecurringTaskRequestDTO {

    @NotBlank(message = "Title is required")
    @Size(max = 100, message = "Title must not exceed 100 characters")
    private String title;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @NotBlank(message = "Frequency is required")
    @Pattern(
            regexp = "DAILY|WEEKLY|BIWEEKLY|MONTHLY|^[0-9*/ ,-]+$",
            message = "Invalid frequency format"
    )
    private String frequency;

    @NotNull(message = "Subject is required")
    private Long subjectId;
}

package com.redcheck.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record EventCategoryRequestDTO(
        @NotBlank(message = "Name is required")
        @Size(max = 50, message = "Name must not exceed 50 characters")
        String name,

        @NotBlank(message = "Color is required")
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Color must be a hex code like #4F46E5")
        String color
) {}

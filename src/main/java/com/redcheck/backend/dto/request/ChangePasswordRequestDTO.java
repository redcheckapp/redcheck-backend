package com.redcheck.backend.dto.request;

import lombok.Builder;

@Builder
public record ChangePasswordRequestDTO(
        String currentPassword,
        String newPassword
) {}

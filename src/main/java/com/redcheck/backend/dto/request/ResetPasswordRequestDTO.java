package com.redcheck.backend.dto.request;

import lombok.Builder;

@Builder
public record ResetPasswordRequestDTO(
        String token,
        String newPassword
) {}

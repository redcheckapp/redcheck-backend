package com.redcheck.backend.dto.response;

import lombok.Builder;

@Builder
public record AuthResponseDTO(
        String token
) {}
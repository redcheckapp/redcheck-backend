package com.redcheck.backend.dto.request;

import lombok.Builder;

@Builder
public record UpdateAliasRequestDTO(
        String alias
) {}

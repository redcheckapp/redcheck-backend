package com.redcheck.backend.dto.response;

import lombok.Builder;

import java.time.Instant;

// GET /admin/system. buildTime/gitSha identify exactly which build is
// running — the point being to never again assume a fix is deployed when
// it isn't. gitSha is "unknown" for local/compose builds (only CI passes it).
@Builder
public record SystemStatusResponseDTO(
        String version,
        Instant buildTime,
        String gitSha,
        Instant startedAt,
        long uptimeSeconds,
        String javaVersion,
        long memoryUsedMb,
        long memoryMaxMb,
        boolean aiEngineReachable,
        Long aiEngineLatencyMs
) {}

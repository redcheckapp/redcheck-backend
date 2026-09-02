package com.redcheck.backend.dto.response;

import lombok.Builder;

import java.time.LocalDate;

@Builder
public record ProgressRecordResponseDTO(
        LocalDate date,
        int totalTasks,
        int completedTasks,
        double completionRate
) {}
package com.redcheck.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProgressRecordResponseDTO {

    private LocalDate date;
    private int totalTasks;
    private int completedTasks;
    private double completionRate;
}
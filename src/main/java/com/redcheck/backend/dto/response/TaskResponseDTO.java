package com.redcheck.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskResponseDTO {

    private Long id;
    private String title;
    private String description;
    private LocalDateTime assignedDate;
    private LocalDateTime deadline;
    private LocalDateTime completedDate;
    private boolean completed; // it can be inferred from completedDate
    private Long subjectId;
}

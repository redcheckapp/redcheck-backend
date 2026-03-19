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
public class RecurringTaskResponseDTO {

    private Long id;
    private String title;
    private String description;
    private String frequency;
    private boolean active;
    private LocalDateTime createdDate;
    private LocalDateTime latestGeneratedDate;
    private Long subjectId;
}

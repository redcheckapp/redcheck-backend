package com.redcheck.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubjectResponseDTO {

    private Long id;
    private String name;
    private String description;
    private boolean deleted;
    private boolean archived;
}

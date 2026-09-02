package com.redcheck.backend.controller;

import com.redcheck.backend.dto.update.SubjectArchiveDTO;
import com.redcheck.backend.dto.request.SubjectRequestDTO;
import com.redcheck.backend.dto.response.SubjectResponseDTO;
import com.redcheck.backend.entity.User;
import com.redcheck.backend.service.SubjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/subjects")
@RequiredArgsConstructor
@CrossOrigin(origins = "${app.cors.allowed-origins}")
public class SubjectController {

    private final SubjectService subjectService;

    @GetMapping
    public ResponseEntity<List<SubjectResponseDTO>> getAll(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(required = false) Boolean archived,
            @RequestParam(required = false) Boolean deleted) {

        List<SubjectResponseDTO> response = subjectService.getAllSubjects(currentUser, archived, deleted);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<SubjectResponseDTO> create(
            @Valid @RequestBody SubjectRequestDTO requestDTO,
            @AuthenticationPrincipal User currentUser) {

        SubjectResponseDTO responseDTO = subjectService.createSubject(requestDTO, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SubjectResponseDTO> modify(
                @PathVariable Long id,
            @Valid @RequestBody SubjectRequestDTO requestDTO,
            @AuthenticationPrincipal User currentUser) {

        SubjectResponseDTO responseDTO = subjectService.modifySubject(id, requestDTO, currentUser);
        return ResponseEntity.ok(responseDTO);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {

        subjectService.deleteSubject(id, currentUser);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/force")
    public ResponseEntity<Void> hardDelete(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {

        subjectService.hardDeleteSubject(id, currentUser);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/restore")
    public ResponseEntity<SubjectResponseDTO> restore(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {

        SubjectResponseDTO responseDTO = subjectService.restoreSubject(id, currentUser);
        return ResponseEntity.ok(responseDTO);
    }

    @PatchMapping("/{id}/archive")
    public ResponseEntity<SubjectResponseDTO> archive(
            @PathVariable Long id,
            @Valid @RequestBody SubjectArchiveDTO requestDTO,
            @AuthenticationPrincipal User currentUser) {

        SubjectResponseDTO responseDTO = subjectService.archiveSubject(id, requestDTO, currentUser);
        return ResponseEntity.ok(responseDTO);
    }
}
package com.redcheck.backend.service;

import com.redcheck.backend.dto.update.SubjectArchiveDTO;
import com.redcheck.backend.dto.request.SubjectRequestDTO;
import com.redcheck.backend.dto.response.SubjectResponseDTO;
import com.redcheck.backend.entity.Subject;
import com.redcheck.backend.entity.User;
import com.redcheck.backend.exception.SubjectAlreadyExistsException;
import com.redcheck.backend.exception.SubjectNotFoundException;
import com.redcheck.backend.exception.SubjectNotOwnedException;
import com.redcheck.backend.repository.SubjectRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubjectService {

    private final SubjectRepository subjectRepository;

    public List<SubjectResponseDTO> getAllSubjects(User currentUser, Boolean archived, Boolean deleted) {
        List<Subject> subjects;

        // 1. If the trash is explicitly requested
        if (deleted != null && deleted) {
            subjects = subjectRepository.findAllByUserAndDeletedTrue(currentUser);
        }
        // 2. Normal filters (ensuring trash is excluded with AndDeletedFalse)
        else if (archived != null) {
            subjects = subjectRepository.findAllByUserAndArchivedAndDeletedFalse(currentUser, archived);
        } else {
            subjects = subjectRepository.findAllByUserAndDeletedFalse(currentUser);
        }

        return subjects.stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public SubjectResponseDTO createSubject(SubjectRequestDTO requestDTO, User currentUser) {

        if (subjectRepository.existsByNameAndUser(requestDTO.name(), currentUser)) {
            throw new SubjectAlreadyExistsException(requestDTO.name());
        }

        Subject subject = Subject.builder()
                .name(requestDTO.name())
                .description(requestDTO.description())
                .archived(false)
                .user(currentUser)
                .build();

        subjectRepository.save(subject);

        return toResponseDTO(subject);
    }

    @Transactional
    public SubjectResponseDTO modifySubject(Long id, SubjectRequestDTO requestDTO, User currentUser) {

        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new SubjectNotFoundException(id));

        if (!subject.getUser().getId().equals(currentUser.getId())) {
            throw new SubjectNotOwnedException();
        }

        if (subjectRepository.existsByNameAndUserAndIdNot(requestDTO.name(), currentUser, id)) {
            throw new SubjectAlreadyExistsException(requestDTO.name());
        }

        subject.setName(requestDTO.name());
        subject.setDescription(requestDTO.description());

        subjectRepository.save(subject);

        return toResponseDTO(subject);
    }

    @Transactional
    public void deleteSubject(Long id, User currentUser) {

        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new SubjectNotFoundException(id));

        if (!subject.getUser().getId().equals(currentUser.getId())) {
            throw new SubjectNotOwnedException();
        }

        subject.setDeleted(true);
        subject.setDeletedAt(LocalDateTime.now());
        subjectRepository.save(subject);
    }

    @Transactional
    public void hardDeleteSubject(Long id, User currentUser) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new SubjectNotFoundException(id));

        if (!subject.getUser().getId().equals(currentUser.getId())) {
            throw new SubjectNotOwnedException();
        }

        if (!subject.isDeleted()) {
            throw new IllegalStateException("The subject must be in the recycle bin in order to be deleted");
        }

        subjectRepository.delete(subject);
    }

    @Transactional
    public SubjectResponseDTO restoreSubject(Long id, User currentUser) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new SubjectNotFoundException(id));

        if (!subject.getUser().getId().equals(currentUser.getId())) {
            throw new SubjectNotOwnedException();
        }

        subject.setDeleted(false);
        subject.setDeletedAt(null);
        subjectRepository.save(subject);

        return toResponseDTO(subject);
    }

    @Transactional
    public SubjectResponseDTO archiveSubject(Long id, SubjectArchiveDTO requestDTO, User currentUser) {

        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new SubjectNotFoundException(id));

        if (!subject.getUser().getId().equals(currentUser.getId())) {
            throw new SubjectNotOwnedException();
        }

        subject.setArchived(requestDTO.archived());

        subjectRepository.save(subject);

        return toResponseDTO(subject);
    }

    private SubjectResponseDTO toResponseDTO(Subject subject) {
        return SubjectResponseDTO.builder()
                .id(subject.getId())
                .name(subject.getName())
                .description(subject.getDescription())
                .archived(subject.isArchived())
                .deleted(subject.isDeleted())
                .build();
    }
}
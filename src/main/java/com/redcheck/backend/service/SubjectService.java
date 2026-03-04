package com.redcheck.backend.service;

import com.redcheck.backend.dto.SubjectArchiveDTO;
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

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubjectService {

    private final SubjectRepository subjectRepository;

    public List<SubjectResponseDTO> getAllSubjects(User currentUser, Boolean archived) {
        List<Subject> subjects = (archived != null)
                ? subjectRepository.findAllByUserAndArchived(currentUser, archived)
                : subjectRepository.findAllByUser(currentUser);

        return subjects.stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public SubjectResponseDTO createSubject(SubjectRequestDTO requestDTO, User currentUser){

        if(subjectRepository.existsByNameAndUser(requestDTO.getName(), currentUser))
            throw new SubjectAlreadyExistsException(requestDTO.getName());

        Subject subject = Subject.builder()
                .name(requestDTO.getName())
                .description(requestDTO.getDescription())
                .archived(false)
                .user(currentUser)
                .build();

        subjectRepository.save(subject);

        return toResponseDTO(subject);
    }

    @Transactional
    public SubjectResponseDTO modifySubject(Long id, SubjectRequestDTO requestDTO, User currentUser){

        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new SubjectNotFoundException(id));

        if(!subject.getUser().getId().equals(currentUser.getId()))
            throw new SubjectNotOwnedException();

        if(subjectRepository.existsByNameAndUserAndIdNot(requestDTO.getName(), currentUser, id))
            throw new SubjectAlreadyExistsException(requestDTO.getName());

        subject.setName(requestDTO.getName());
        subject.setDescription(requestDTO.getDescription());

        subjectRepository.save(subject);

        return toResponseDTO(subject);
    }

    @Transactional
    public void deleteSubject(Long id, User currentUser){

        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new SubjectNotFoundException(id));

        if(!subject.getUser().getId().equals(currentUser.getId()))
            throw new SubjectNotOwnedException();

        subjectRepository.delete(subject);
    }

    @Transactional
    public SubjectResponseDTO archiveSubject(Long id, SubjectArchiveDTO requestDTO, User currentUser){

        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new SubjectNotFoundException(id));

        if(!subject.getUser().getId().equals(currentUser.getId()))
            throw new SubjectNotOwnedException();

        subject.setArchived(requestDTO.isArchived());

        subjectRepository.save(subject);

        return toResponseDTO(subject);
    }

    private SubjectResponseDTO toResponseDTO(Subject subject) {
        return SubjectResponseDTO.builder()
                .id(subject.getId())
                .name(subject.getName())
                .description(subject.getDescription())
                .archived(subject.isArchived())
                .build();
    }
}

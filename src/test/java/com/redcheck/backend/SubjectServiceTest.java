package com.redcheck.backend;

import com.redcheck.backend.dto.request.SubjectRequestDTO;
import com.redcheck.backend.dto.response.SubjectResponseDTO;
import com.redcheck.backend.dto.update.SubjectArchiveDTO;
import com.redcheck.backend.entity.Subject;
import com.redcheck.backend.entity.User;
import com.redcheck.backend.exception.SubjectAlreadyExistsException;
import com.redcheck.backend.exception.SubjectNotFoundException;
import com.redcheck.backend.exception.SubjectNotOwnedException;
import com.redcheck.backend.repository.SubjectRepository;
import com.redcheck.backend.service.SubjectService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unitary Tests - SubjectService")
public class SubjectServiceTest {

    @Mock
    private SubjectRepository subjectRepository;

    @InjectMocks
    private SubjectService subjectService;

    private User user;
    private User hacker;
    private Subject mockSubject;
    private SubjectRequestDTO mockRequestSubjectDTO;
    private SubjectArchiveDTO mockArchiveSubjectDTO;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .username("user")
                .email("user@redcheck.com")
                .password("redcheckUser")
                .build();
        user.setId(1L);

        hacker = User.builder()
                .username("hacker")
                .email("hacker@redcheck.com")
                .password("redcheckHacker")
                .build();
        hacker.setId(2L);

        mockSubject = Subject.builder()
                .name("subject")
                .user(user)
                .build();
        mockSubject.setId(10L);

        mockRequestSubjectDTO = SubjectRequestDTO.builder()
                .name("new subject name")
                .description("new subject description")
                .build();

        mockArchiveSubjectDTO = SubjectArchiveDTO.builder()
                .archived(true)
                .build();
    }

    @Nested
    @DisplayName("Method: getAllSubjects")
    class GetAllSubjectsTests {

        @Test
        @DisplayName("When no filter applied should return all subjects")
        void getAllSubjects_WhenNoFilterApplied_ShouldReturnAllSujects() {
            // GIVEN
            when(subjectRepository.findAllByUser(user))
                    .thenReturn(Collections.singletonList(mockSubject));

            // WHEN
            List<SubjectResponseDTO> result = subjectService.getAllSubjects(user, null);

            // THEN
            assertFalse(result.isEmpty());
            assertEquals(1, result.size());
            assertEquals(mockSubject.getId(), result.get(0).getId());
            verify(subjectRepository, times(1)).findAllByUser(user);
        }
    }

    @Nested
    @DisplayName("Method: createSubject")
    class CreateSubjectTests {

        @Test
        @DisplayName("When subject does not exist should save and return subject")
        void createSubject_WhenSubjectDoesNotExist_ShouldSaveAndReturnSubject() {
            // GIVEN
            when(subjectRepository.existsByNameAndUser(mockRequestSubjectDTO.getName(), user))
                    .thenReturn(false);

            when(subjectRepository.save(any(Subject.class)))
                    .thenReturn(mockSubject);

            // WHEN
            SubjectResponseDTO result = subjectService.createSubject(mockRequestSubjectDTO, user);

            // THEN
            assertNotNull(result);
            verify(subjectRepository, times(1)).existsByNameAndUser(mockRequestSubjectDTO.getName(), user);
            verify(subjectRepository, times(1)).save(any(Subject.class));
        }

        @Test
        @DisplayName("When subject exists should throw exception")
        void createSubject_WhenSubjectExists_ShouldThrowException() {
            // GIVEN
            when(subjectRepository.existsByNameAndUser(mockRequestSubjectDTO.getName(), user))
                    .thenReturn(true);

            // WHEN
            assertThrows(SubjectAlreadyExistsException.class, () -> {
                subjectService.createSubject(mockRequestSubjectDTO, user);
            });

            // THEN
            verify(subjectRepository, times(1)).existsByNameAndUser(mockRequestSubjectDTO.getName(), user);
            verify(subjectRepository, never()).save(any(Subject.class));
        }
    }

    @Nested
    @DisplayName("Method: modifySubject")
    class UpdateSubjectTests {

        @Test
        @DisplayName("When subject is owned should update and return subject")
        void updateModify_WhenSubjectIsOwned_ShouldUpdateAndReturnSubject() {
            // GIVEN
            when(subjectRepository.findById(mockSubject.getId()))
                    .thenReturn(Optional.of(mockSubject));

            when(subjectRepository.existsByNameAndUserAndIdNot(mockRequestSubjectDTO.getName(), user, mockSubject.getId()))
                    .thenReturn(false);

            // WHEN
            SubjectResponseDTO result = subjectService.modifySubject(mockSubject.getId(), mockRequestSubjectDTO, user);

            // THEN
            assertNotNull(result);
            assertEquals(result.getName(), mockRequestSubjectDTO.getName());
            assertEquals(result.getDescription(), mockRequestSubjectDTO.getDescription());
            verify(subjectRepository, times(1)).findById(mockSubject.getId());
            verify(subjectRepository, times(1)).existsByNameAndUserAndIdNot(mockRequestSubjectDTO.getName(), user, mockSubject.getId());
            verify(subjectRepository, times(1)).save(any(Subject.class));
        }

        @Test
        @DisplayName("When subject is not owned should throw exception")
        void updateModify_WhenSubjectIsNotOwned_ShouldThrowException() {
            // GIVEN
            when(subjectRepository.findById(mockSubject.getId()))
                    .thenReturn(Optional.of(mockSubject));

            // WHEN
            assertThrows(SubjectNotOwnedException.class, () -> {
                subjectService.modifySubject(mockSubject.getId(), mockRequestSubjectDTO, hacker);
            });

            // THEN
            verify(subjectRepository, times(1)).findById(mockSubject.getId());
            verify(subjectRepository, never()).existsByNameAndUserAndIdNot(mockRequestSubjectDTO.getName(), hacker, mockSubject.getId());
            verify(subjectRepository, never()).save(any(Subject.class));
        }

        @Test
        @DisplayName("When subject does not exist should throw exception")
        void updateModify_WhenSubjectDoesNotExist_ShouldThrowException() {
            // GIVEN
            when(subjectRepository.findById(mockSubject.getId()))
                    .thenReturn(Optional.empty());

            // WHEN
            assertThrows(SubjectNotFoundException.class, () -> {
                subjectService.modifySubject(mockSubject.getId(), mockRequestSubjectDTO, user);
            });

            // THEN
            verify(subjectRepository, times(1)).findById(mockSubject.getId());
            verify(subjectRepository, never()).save(any(Subject.class));
        }
    }

    @Nested
    @DisplayName("Method: deleteSubject")
    class DeleteSubjectTests {

        @Test
        @DisplayName("When subject is owned should delete subject")
        void deleteSubject_WhenSubjectIsOwned_ShouldDeleteSubject() {
            // GIVEN
            when(subjectRepository.findById(mockSubject.getId()))
                    .thenReturn(Optional.of(mockSubject));

            // WHEN
            subjectService.deleteSubject(mockSubject.getId(), user);

            // THEN
            verify(subjectRepository, times(1)).findById(mockSubject.getId());
            verify(subjectRepository, times(1)).delete(any(Subject.class));
        }
    }

    @Nested
    @DisplayName("Method: archiveSubject")
    class ArchiveSubjectTests {

        @Test
        @DisplayName("When subject is owned should archive and return subject")
        void updateSubject_WhenSubjectIsOwned_ShouldArchiveAndReturnSubject() {
            // GIVEN
            when(subjectRepository.findById(mockSubject.getId()))
                    .thenReturn(Optional.of(mockSubject));

            // WHEN
            SubjectResponseDTO result = subjectService.archiveSubject(mockSubject.getId(), mockArchiveSubjectDTO, user);

            // THEN
            assertNotNull(result);
            assertEquals(result.getName(), mockSubject.getName());
            assertEquals(result.getDescription(), mockSubject.getDescription());
            assertTrue(result.isArchived());
            verify(subjectRepository, times(1)).findById(mockSubject.getId());
            verify(subjectRepository, times(1)).save(any(Subject.class));
        }
    }
}

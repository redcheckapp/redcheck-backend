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

import java.time.LocalDateTime;
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
            when(subjectRepository.findAllByUserAndDeletedFalse(user))
                    .thenReturn(Collections.singletonList(mockSubject));

            // WHEN
            List<SubjectResponseDTO> result = subjectService.getAllSubjects(user, null, null);

            // THEN
            assertFalse(result.isEmpty());
            assertEquals(1, result.size());
            assertEquals(mockSubject.getId(), result.get(0).id());
            verify(subjectRepository, times(1)).findAllByUserAndDeletedFalse(user);
        }
    }

    @Nested
    @DisplayName("Method: createSubject")
    class CreateSubjectTests {

        @Test
        @DisplayName("When subject does not exist should save and return subject")
        void createSubject_WhenSubjectDoesNotExist_ShouldSaveAndReturnSubject() {
            // GIVEN
            when(subjectRepository.existsByNameAndUser(mockRequestSubjectDTO.name(), user))
                    .thenReturn(false);

            when(subjectRepository.save(any(Subject.class)))
                    .thenReturn(mockSubject);

            // WHEN
            SubjectResponseDTO result = subjectService.createSubject(mockRequestSubjectDTO, user);

            // THEN
            assertNotNull(result);
            verify(subjectRepository, times(1)).existsByNameAndUser(mockRequestSubjectDTO.name(), user);
            verify(subjectRepository, times(1)).save(any(Subject.class));
        }

        @Test
        @DisplayName("When subject exists should throw exception")
        void createSubject_WhenSubjectExists_ShouldThrowException() {
            // GIVEN
            when(subjectRepository.existsByNameAndUser(mockRequestSubjectDTO.name(), user))
                    .thenReturn(true);

            // WHEN
            assertThrows(SubjectAlreadyExistsException.class, () -> {
                subjectService.createSubject(mockRequestSubjectDTO, user);
            });

            // THEN
            verify(subjectRepository, times(1)).existsByNameAndUser(mockRequestSubjectDTO.name(), user);
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

            when(subjectRepository.existsByNameAndUserAndIdNot(mockRequestSubjectDTO.name(), user, mockSubject.getId()))
                    .thenReturn(false);

            // WHEN
            SubjectResponseDTO result = subjectService.modifySubject(mockSubject.getId(), mockRequestSubjectDTO, user);

            // THEN
            assertNotNull(result);
            assertEquals(result.name(), mockRequestSubjectDTO.name());
            assertEquals(result.description(), mockRequestSubjectDTO.description());
            verify(subjectRepository, times(1)).findById(mockSubject.getId());
            verify(subjectRepository, times(1)).existsByNameAndUserAndIdNot(mockRequestSubjectDTO.name(), user, mockSubject.getId());
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
            verify(subjectRepository, never()).existsByNameAndUserAndIdNot(mockRequestSubjectDTO.name(), hacker, mockSubject.getId());
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
    @DisplayName("Method: deleteSubject (Soft Delete)")
    class DeleteSubjectTests {

        @Test
        @DisplayName("When subject is owned should soft delete subject")
        void deleteSubject_WhenSubjectIsOwned_ShouldSoftDeleteSubject() {
            // GIVEN
            when(subjectRepository.findById(mockSubject.getId()))
                    .thenReturn(Optional.of(mockSubject));

            // WHEN
            subjectService.deleteSubject(mockSubject.getId(), user);

            // THEN
            assertTrue(mockSubject.isDeleted());
            assertNotNull(mockSubject.getDeletedAt());

            verify(subjectRepository, times(1)).findById(mockSubject.getId());
            verify(subjectRepository, times(1)).save(mockSubject);
            verify(subjectRepository, never()).delete(any(Subject.class));
        }
    }

    @Nested
    @DisplayName("Method: restoreSubject")
    class RestoreSubjectTests {

        @Test
        @DisplayName("When subject is owned should restore subject from trash")
        void restoreSubject_WhenSubjectIsOwned_ShouldRestoreSubject() {
            // GIVEN
            mockSubject.setDeleted(true);
            mockSubject.setDeletedAt(LocalDateTime.now().minusHours(5));

            when(subjectRepository.findById(mockSubject.getId()))
                    .thenReturn(Optional.of(mockSubject));

            // WHEN
            SubjectResponseDTO result = subjectService.restoreSubject(mockSubject.getId(), user);

            // THEN
            assertNotNull(result);
            assertFalse(mockSubject.isDeleted());
            assertNull(mockSubject.getDeletedAt());

            verify(subjectRepository, times(1)).save(mockSubject);
        }
    }

    @Nested
    @DisplayName("Method: hardDeleteSubject")
    class HardDeleteSubjectTests {

        @Test
        @DisplayName("When subject is owned should permanently delete subject")
        void hardDeleteSubject_WhenSubjectIsOwned_ShouldPermanentlyDelete() {
            // GIVEN
            when(subjectRepository.findById(mockSubject.getId()))
                    .thenReturn(Optional.of(mockSubject));
            mockSubject.setDeleted(true);

            // WHEN
            subjectService.hardDeleteSubject(mockSubject.getId(), user);

            // THEN
            verify(subjectRepository, times(1)).delete(mockSubject);
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
            assertEquals(result.name(), mockSubject.getName());
            assertEquals(result.description(), mockSubject.getDescription());
            assertTrue(result.archived());
            verify(subjectRepository, times(1)).findById(mockSubject.getId());
            verify(subjectRepository, times(1)).save(any(Subject.class));
        }
    }
}
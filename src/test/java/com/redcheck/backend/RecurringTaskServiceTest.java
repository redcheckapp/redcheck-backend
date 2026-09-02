package com.redcheck.backend;

import com.redcheck.backend.dto.request.RecurringTaskRequestDTO;
import com.redcheck.backend.dto.response.RecurringTaskResponseDTO;
import com.redcheck.backend.dto.update.RecurringTaskActiveDTO;
import com.redcheck.backend.entity.RecurringTask;
import com.redcheck.backend.entity.Subject;
import com.redcheck.backend.entity.User;
import com.redcheck.backend.exception.RecurringTaskNotOwnedException;
import com.redcheck.backend.exception.SubjectNotOwnedException;
import com.redcheck.backend.repository.RecurringTaskRepository;
import com.redcheck.backend.repository.SubjectRepository;
import com.redcheck.backend.repository.TaskRepository;
import com.redcheck.backend.service.RecurringTaskService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unitary Tests - RecurringTaskService")
public class RecurringTaskServiceTest {

    @Mock
    private RecurringTaskRepository recurringTaskRepository;

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private RecurringTaskService recurringTaskService;

    private User currentUser;
    private User hackerUser;
    private Subject mockSubject;
    private RecurringTask mockRecurringTask;
    private RecurringTaskRequestDTO requestDTO;
    private RecurringTaskActiveDTO activeDTO;

    @BeforeEach
    void setUp() {
        currentUser = User.builder().id(1L).username("user").build();
        hackerUser = User.builder().id(2L).username("hacker").build();

        mockSubject = Subject.builder()
                .id(10L)
                .name("Math")
                .user(currentUser)
                .build();

        mockRecurringTask = RecurringTask.builder()
                .id(100L)
                .title("Study Math")
                .description("Review formulas")
                .frequency("WEEKLY")
                .active(true)
                .subject(mockSubject)
                .createdDate(LocalDateTime.now())
                .build();

        requestDTO = RecurringTaskRequestDTO.builder()
                .title("New Title")
                .description("New Desc")
                .frequency("DAILY")
                .subjectId(10L)
                .build();

        activeDTO = RecurringTaskActiveDTO.builder()
                .active(false)
                .build();
    }

    @Nested
    @DisplayName("Method: getAllRecurringTask")
    class GetAllTests {

        @Test
        @DisplayName("When active is provided should filter by user and active flag")
        void getAll_WhenActiveProvided_ShouldFilterByUserAndActive() {
            // GIVEN
            when(recurringTaskRepository.findAllBySubject_User_IdAndActive(currentUser.getId(), true))
                    .thenReturn(Collections.singletonList(mockRecurringTask));

            // WHEN
            List<RecurringTaskResponseDTO> result = recurringTaskService.getAllRecurringTask(currentUser, null, true);

            // THEN
            assertEquals(1, result.size());
            assertEquals("Study Math", result.get(0).title());
            verify(recurringTaskRepository, times(1)).findAllBySubject_User_IdAndActive(currentUser.getId(), true);
            verify(recurringTaskRepository, never()).findAllBySubject_User_Id(anyLong());
        }

        @Test
        @DisplayName("When active is null should filter only by user")
        void getAll_WhenActiveIsNull_ShouldFilterOnlyByUser() {
            // GIVEN
            when(recurringTaskRepository.findAllBySubject_User_Id(currentUser.getId()))
                    .thenReturn(Collections.singletonList(mockRecurringTask));

            // WHEN
            List<RecurringTaskResponseDTO> result = recurringTaskService.getAllRecurringTask(currentUser, null, null);

            // THEN
            assertEquals(1, result.size());
            verify(recurringTaskRepository, times(1)).findAllBySubject_User_Id(currentUser.getId());
        }
    }

    @Nested
    @DisplayName("Method: createRecurringTask")
    class CreateTests {

        @Test
        @DisplayName("When subject is owned should create and return task")
        void create_WhenSubjectIsOwned_ShouldCreateAndReturnTask() {
            // GIVEN
            when(subjectRepository.findById(mockSubject.getId()))
                    .thenReturn(Optional.of(mockSubject));

            when(recurringTaskRepository.save(any(RecurringTask.class)))
                    .thenReturn(mockRecurringTask);

            // WHEN
            RecurringTaskResponseDTO result = recurringTaskService.createRecurringTask(mockSubject.getId(), requestDTO, currentUser);

            // THEN
            assertNotNull(result);
            assertEquals(requestDTO.title(), result.title());
            verify(subjectRepository, times(1)).findById(mockSubject.getId());
            verify(recurringTaskRepository, times(1)).save(any(RecurringTask.class));
        }

        @Test
        @DisplayName("When subject is not owned should throw exception")
        void create_WhenSubjectIsNotOwned_ShouldThrowException() {
            // GIVEN
            when(subjectRepository.findById(mockSubject.getId()))
                    .thenReturn(Optional.of(mockSubject));

            // WHEN & THEN
            assertThrows(SubjectNotOwnedException.class, () -> {
                recurringTaskService.createRecurringTask(mockSubject.getId(), requestDTO, hackerUser);
            });

            verify(recurringTaskRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Method: updateRecurringTask")
    class UpdateTests {

        @Test
        @DisplayName("When all data is valid and owned should update task")
        void update_WhenDataIsValid_ShouldUpdateTask() {
            // GIVEN
            when(subjectRepository.findById(mockSubject.getId()))
                    .thenReturn(Optional.of(mockSubject));

            when(recurringTaskRepository.findById(mockRecurringTask.getId()))
                    .thenReturn(Optional.of(mockRecurringTask));

            when(subjectRepository.findById(requestDTO.subjectId()))
                    .thenReturn(Optional.of(mockSubject));

            // WHEN
            RecurringTaskResponseDTO result = recurringTaskService.updateRecurringTask(mockSubject.getId(), mockRecurringTask.getId(), requestDTO, currentUser);

            // THEN
            assertNotNull(result);
            assertEquals("New Title", result.title());
            assertEquals("New Desc", result.description());
            verify(recurringTaskRepository, times(1)).save(mockRecurringTask);
        }

        @Test
        @DisplayName("When recurring task belongs to another subject should throw exception")
        void update_WhenTaskBelongsToAnotherSubject_ShouldThrowException() {
            // GIVEN
            Subject anotherSubject = Subject.builder().id(99L).user(currentUser).build();
            mockRecurringTask.setSubject(anotherSubject);

            when(subjectRepository.findById(mockSubject.getId()))
                    .thenReturn(Optional.of(mockSubject));

            when(recurringTaskRepository.findById(mockRecurringTask.getId()))
                    .thenReturn(Optional.of(mockRecurringTask));

            // WHEN & THEN
            assertThrows(RecurringTaskNotOwnedException.class, () -> {
                recurringTaskService.updateRecurringTask(mockSubject.getId(), mockRecurringTask.getId(), requestDTO, currentUser);
            });

            verify(recurringTaskRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Method: deleteRecurringTask")
    class DeleteTests {

        @Test
        @DisplayName("When valid should detach tasks and delete recurring task")
        void delete_WhenValid_ShouldDetachAndDelete() {
            // GIVEN
            when(subjectRepository.findById(mockSubject.getId()))
                    .thenReturn(Optional.of(mockSubject));

            when(recurringTaskRepository.findById(mockRecurringTask.getId()))
                    .thenReturn(Optional.of(mockRecurringTask));

            // WHEN
            recurringTaskService.deleteRecurringTask(mockSubject.getId(), mockRecurringTask.getId(), currentUser);

            // THEN
            verify(taskRepository, times(1)).detachFromRecurringTask(mockRecurringTask);
            verify(recurringTaskRepository, times(1)).delete(mockRecurringTask);
        }
    }

    @Nested
    @DisplayName("Method: activateRecurringTask")
    class ActivateTests {

        @Test
        @DisplayName("When valid should update active status")
        void activate_WhenValid_ShouldUpdateActiveStatus() {
            // GIVEN
            when(subjectRepository.findById(mockSubject.getId()))
                    .thenReturn(Optional.of(mockSubject));

            when(recurringTaskRepository.findById(mockRecurringTask.getId()))
                    .thenReturn(Optional.of(mockRecurringTask));

            // WHEN
            RecurringTaskResponseDTO result = recurringTaskService.activateRecurringTask(mockSubject.getId(), mockRecurringTask.getId(), activeDTO, currentUser);

            // THEN
            assertFalse(result.active());
            verify(recurringTaskRepository, times(1)).save(mockRecurringTask);
        }
    }
}
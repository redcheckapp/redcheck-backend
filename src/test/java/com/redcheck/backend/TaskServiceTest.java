package com.redcheck.backend;

import com.redcheck.backend.dto.request.TaskRequestDTO;
import com.redcheck.backend.dto.response.TaskResponseDTO;
import com.redcheck.backend.dto.update.TaskCompleteDTO;
import com.redcheck.backend.entity.Subject;
import com.redcheck.backend.entity.Task;
import com.redcheck.backend.entity.User;
import com.redcheck.backend.exception.SubjectNotFoundException;
import com.redcheck.backend.exception.SubjectNotOwnedException;
import com.redcheck.backend.exception.TaskNotOwnedException;
import com.redcheck.backend.repository.SubjectRepository;
import com.redcheck.backend.repository.TaskRepository;
import com.redcheck.backend.service.TaskService;
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
@DisplayName("Unitary Tests - TaskService")
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private SubjectRepository subjectRepository;

    @InjectMocks
    private TaskService taskService;

    private User user;
    private User hacker;
    private Task mockTask;
    private Subject mockSubject;
    private TaskRequestDTO mockRequestTaskDTO;
    private TaskCompleteDTO mockCompleteTaskDTO;
    private TaskCompleteDTO mockIncompleteTaskDTO;

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

        mockTask = Task.builder()
                .title("task")
                .subject(mockSubject)
                .build();
        mockTask.setId(100L);

        mockRequestTaskDTO = TaskRequestDTO.builder()
                .title("new task title")
                .description("new task description")
                .deadline(LocalDateTime.now())
                .build();

        mockCompleteTaskDTO = TaskCompleteDTO.builder()
                .completed(true)
                .build();

        mockIncompleteTaskDTO = TaskCompleteDTO.builder()
                .completed(false)
                .build();
    }

    @Nested
    @DisplayName("Method: getAllTask")
    class GetAllTaskTests {

        @Test
        @DisplayName("When no filters applied should return all tasks")
        void getAllTask_WhenNoFiltersApplied_ShouldReturnAllTasks() {
            // GIVEN: Repository returns a list of tasks for the current user
            when(taskRepository.findAllBySubject_User_IdAndDeletedFalse(user.getId()))
                    .thenReturn(Collections.singletonList(mockTask));

            // WHEN: Fetching tasks without overdue or completed filters
            List<TaskResponseDTO> result = taskService.getAllTask(user, mockSubject.getId(), null, null, null);

            // THEN: The list should contain the mapped DTO
            assertFalse(result.isEmpty());
            assertEquals(1, result.size());
            assertEquals(mockTask.getId(), result.get(0).id());
            verify(taskRepository, times(1)).findAllBySubject_User_IdAndDeletedFalse(user.getId());        }
    }

    @Nested
    @DisplayName("Method: createTask")
    class CreateTaskTest {

        @Test
        @DisplayName("When subject exists and is owned should save and return task")
        void createTask_WhenSubjectExistsAndIsOwned_ShouldSaveAndReturnTask() {
            // GIVEN: Subject exists and belongs to the current user
            when(subjectRepository.findById(mockSubject.getId()))
                    .thenReturn(Optional.ofNullable(mockSubject));

            // WHEN: Creating a task
            TaskResponseDTO result = taskService.createTask(mockSubject.getId(), mockRequestTaskDTO, user);

            // THEN: Task is saved and returned as DTO
            assertNotNull(result);
            assertEquals(mockSubject.getId(), result.subjectId());
            verify(subjectRepository, times(1)).findById(mockSubject.getId());
            verify(taskRepository, times(1)).save(any(Task.class));
        }

        @Test
        @DisplayName("When subject belongs to another user should throw exception")
        void createTask_WhenSubjectBelongsToAnotherUser_ShouldThrowException() {
            // GIVEN: Subject exists and belongs to the current user
            when(subjectRepository.findById(mockSubject.getId()))
                    .thenReturn(Optional.ofNullable(mockSubject));

            // WHEN: Creating a task for hacker user in current user's subject
            assertThrows(SubjectNotOwnedException.class, () -> {
                taskService.createTask(mockSubject.getId(), mockRequestTaskDTO, hacker);
            });

            // THEN: subjectRepository.findById() is called once
            verify(subjectRepository, times(1)).findById(mockSubject.getId());
            verify(taskRepository, never()).save(any(Task.class));
        }

        @Test
        @DisplayName("When subject does not exist should throw exception")
        void createTask_WhenSubjectDoesNotExist_ShouldThrowException() {
            // GIVEN: Subject does not exist
            when(subjectRepository.findById(999L))
                    .thenReturn(Optional.empty());

            // WHEN: Creating a task for the current user in non-existing subject
            assertThrows(SubjectNotFoundException.class, () -> {
                taskService.createTask(999L, mockRequestTaskDTO, user);
            });

            // THEN: subjectRepository.findById() is called once
            verify(subjectRepository, times(1)).findById(999L);
            verify(taskRepository, never()).save(any(Task.class));
        }
    }

    // ==========================================
    // TESTS FOR: updateTask
    // ==========================================

    @Nested
    @DisplayName("Method: updateTask")
    class UpdateTaskTest {

        @Test
        void updateTask_WhenTaskIsOwned_ShouldUpdateAndReturnTask() {
            // GIVEN: Subject exists and belongs to the current user with one associated task
            when(subjectRepository.findById(mockSubject.getId()))
                    .thenReturn(Optional.ofNullable(mockSubject));

            when(taskRepository.findById(mockTask.getId()))
                    .thenReturn(Optional.ofNullable(mockTask));

            // WHEN: Updating the task
            TaskResponseDTO result = taskService.updateTask(mockSubject.getId(), mockTask.getId(), mockRequestTaskDTO, user);

            // THEN: The task properties should be updated and saved
            assertNotNull(result);
            assertEquals(result.title(), mockRequestTaskDTO.title());
            assertEquals(result.description(), mockRequestTaskDTO.description());
            assertEquals(result.deadline(), mockRequestTaskDTO.deadline());
            verify(subjectRepository, times(1)).findById(mockSubject.getId());
            verify(taskRepository, times(1)).findById(mockTask.getId());
            verify(taskRepository, times(1)).save(any(Task.class));
        }

        @Test
        void updateTask_WhenTaskBelongsToDifferentSubject_ShouldThrowException() {
            // GIVEN: Task exists but belongs to a different subject
            Subject otherSubject = new Subject();
            otherSubject.setId(20L);
            otherSubject.setUser(user);
            mockTask.setSubject(otherSubject); // Mismatch! Task subject (20) != Request subject (10)

            when(subjectRepository.findById(mockSubject.getId()))
                    .thenReturn(Optional.of(mockSubject));

            when(taskRepository.findById(mockTask.getId()))
                    .thenReturn(Optional.of(mockTask));

            // WHEN: Updating a task for the current user in non-associated subject
            assertThrows(TaskNotOwnedException.class, () -> {
                taskService.updateTask(mockSubject.getId(), mockTask.getId(), mockRequestTaskDTO, user);
            });

            // THEN: subjectRepository.findById() is called once
            verify(subjectRepository, times(1)).findById(mockSubject.getId());
            verify(taskRepository, times(1)).findById(mockTask.getId());
            verify(taskRepository, never()).save(any(Task.class));
        }
    }

    @Nested
    @DisplayName("Method: deleteTask (Soft Delete)")
    class DeleteTaskTest {

        @Test
        @DisplayName("When task is owned should soft delete task")
        void deleteTask_WhenTaskIsOwned_ShouldSoftDeleteTask() {
            // GIVEN
            when(subjectRepository.findById(mockSubject.getId()))
                    .thenReturn(Optional.ofNullable(mockSubject));

            when(taskRepository.findById(mockTask.getId()))
                    .thenReturn(Optional.ofNullable(mockTask));

            // WHEN
            taskService.deleteTask(mockSubject.getId(), mockTask.getId(), user);

            // THEN: verify it was saved with deleted flag and timestamp, NOT hard deleted
            verify(subjectRepository, times(1)).findById(mockSubject.getId());
            verify(taskRepository, times(1)).findById(mockTask.getId());

            assertTrue(mockTask.isDeleted());
            assertNotNull(mockTask.getDeletedAt());

            verify(taskRepository, times(1)).save(mockTask);
            verify(taskRepository, never()).delete(any(Task.class));
        }
    }

    @Nested
    @DisplayName("Method: restoreTask")
    class RestoreTaskTest {

        @Test
        @DisplayName("When task is owned should restore task from trash")
        void restoreTask_WhenTaskIsOwned_ShouldRestoreTask() {
            // GIVEN: A task that is currently deleted
            mockTask.setDeleted(true);
            mockTask.setDeletedAt(LocalDateTime.now().minusHours(2));

            when(subjectRepository.findById(mockSubject.getId()))
                    .thenReturn(Optional.ofNullable(mockSubject));
            when(taskRepository.findById(mockTask.getId()))
                    .thenReturn(Optional.ofNullable(mockTask));

            // WHEN
            TaskResponseDTO result = taskService.restoreTask(mockSubject.getId(), mockTask.getId(), user);

            // THEN
            assertNotNull(result);
            assertFalse(mockTask.isDeleted());
            assertNull(mockTask.getDeletedAt());

            verify(taskRepository, times(1)).save(mockTask);
        }
    }

    @Nested
    @DisplayName("Method: hardDeleteTask")
    class HardDeleteTaskTest {

        @Test
        @DisplayName("When task is owned should permanently delete task")
        void hardDeleteTask_WhenTaskIsOwned_ShouldPermanentlyDelete() {
            // GIVEN
            mockTask.setDeleted(true);
            when(subjectRepository.findById(10L)).thenReturn(Optional.of(mockSubject));
            when(taskRepository.findById(100L)).thenReturn(Optional.of(mockTask));

            // WHEN
            taskService.hardDeleteTask(10L, 100L, user);

            // THEN
            verify(taskRepository, times(1)).delete(mockTask);
        }
    }

    @Nested
    @DisplayName("Method: markTaskAsCompleted")
    class MarkTaskAsCompletedTest {

        @Test
        @DisplayName("When setting to true should set completed date")
        void markTaskAsCompleted_WhenSettingToTrue_ShouldSetCompletedDate() {
            // GIVEN: Subject exists and belongs to the current user with one associated task
            when(subjectRepository.findById(mockSubject.getId()))
                    .thenReturn(Optional.ofNullable(mockSubject));

            when(taskRepository.findById(mockTask.getId()))
                    .thenReturn(Optional.ofNullable(mockTask));

            // WHEN: Marking the task as completed
            TaskResponseDTO result = taskService.markTaskAsCompleted(mockSubject.getId(), mockTask.getId(), mockCompleteTaskDTO, user);

            // THEN: The task properties should be updated and saved
            assertNotNull(result);
            assertNotNull(result.completedDate());
            verify(subjectRepository, times(1)).findById(mockSubject.getId());
            verify(taskRepository, times(1)).findById(mockTask.getId());
            verify(taskRepository, times(1)).save(any(Task.class));
        }

        @Test
        @DisplayName("When setting to false should remove completed date")
        void markTaskAsCompleted_WhenSettingToFalse_ShouldRemoveCompletedDate() {
            // GIVEN: Subject exists and belongs to the current user with one associated task
            when(subjectRepository.findById(mockSubject.getId()))
                    .thenReturn(Optional.ofNullable(mockSubject));

            when(taskRepository.findById(mockTask.getId()))
                    .thenReturn(Optional.ofNullable(mockTask));

            // WHEN: Marking the task as completed
            TaskResponseDTO result = taskService.markTaskAsCompleted(mockSubject.getId(), mockTask.getId(), mockIncompleteTaskDTO, user);

            // THEN: The task properties should be updated and saved
            assertNotNull(result);
            assertNull(result.completedDate());
            verify(subjectRepository, times(1)).findById(mockSubject.getId());
            verify(taskRepository, times(1)).findById(mockTask.getId());
            verify(taskRepository, times(1)).save(any(Task.class));
        }
    }
}
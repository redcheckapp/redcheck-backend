package com.redcheck.backend;

import com.redcheck.backend.repository.RecurringTaskRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.redcheck.backend.entity.RecurringTask;
import com.redcheck.backend.entity.Task;
import com.redcheck.backend.repository.TaskRepository;
import com.redcheck.backend.service.RecurringTaskSchedulerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unitary Tests - RecurringTaskSchedulerService")
public class RecurringTaskSchedulerServiceTest {

    @Mock
    private RecurringTaskRepository recurringTaskRepository;

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private RecurringTaskSchedulerService recurringTaskSchedulerService;

    private RecurringTask mockRecurringTask;
    private Task mockIncompleteTask;
    private Task mockCompleteTask;

    @BeforeEach
    void setUp() {
        // Normal recurring task
        mockRecurringTask = RecurringTask.builder()
                .id(1L)
                .title("Read 10 pages")
                .description("Daily reading habit")
                .frequency("DAILY")
                .active(true)
                .build();

        // Child task incompleted (without completedDate)
        mockIncompleteTask = Task.builder()
                .id(100L)
                .title("Read 10 pages")
                .recurringTask(mockRecurringTask)
                .completedDate(null)
                .build();

        // Child task completed (with completedDate)
        mockCompleteTask = Task.builder()
                .id(101L)
                .title("Read 10 pages")
                .recurringTask(mockRecurringTask)
                .completedDate(LocalDateTime.now().minusDays(1))
                .build();
    }

    @Nested
    @DisplayName("Method: generateTask")
    class GenerateTaskTests {

        @Test
        @DisplayName("When task is never generated before should create task and update date")
        void generateTask_WhenNeverGenerated_ShouldCreateTaskAndUpdateDate() {
            // GIVEN:
            mockRecurringTask.setLatestGeneratedDate(null);

            when(recurringTaskRepository.findAllByActiveTrue())
                    .thenReturn(Collections.singletonList(mockRecurringTask));

            // WHEN:
            recurringTaskSchedulerService.generateTask();

            // THEN:
            verify(recurringTaskRepository, times(1)).findAllByActiveTrue();
            verify(taskRepository, times(1)).save(any(Task.class));
            verify(recurringTaskRepository, times(1)).save(mockRecurringTask);
        }

        @Test
        @DisplayName("When time passed but last task is incomplete should NOT create task but update date")
        void generateTask_WhenLastTaskIncomplete_ShouldNotCreateTaskButUpdateDate() {
            // GIVEN:
            mockRecurringTask.setLatestGeneratedDate(LocalDateTime.now().minusYears(10));

            when(recurringTaskRepository.findAllByActiveTrue())
                    .thenReturn(Collections.singletonList(mockRecurringTask));

            when(taskRepository.findTopByRecurringTaskOrderByAssignedDateDesc(mockRecurringTask))
                    .thenReturn(Optional.of(mockIncompleteTask));

            // WHEN
            recurringTaskSchedulerService.generateTask();

            // THEN:
            verify(taskRepository, never()).save(any(Task.class));
            verify(recurringTaskRepository, times(1)).save(mockRecurringTask);
        }

        @Test
        @DisplayName("When time passed and last task is completed should create task and update date")
        void generateTask_WhenLastTaskCompleted_ShouldCreateTaskAndUpdateDate() {
            // GIVEN:
            mockRecurringTask.setLatestGeneratedDate(LocalDateTime.now().minusYears(10));

            when(recurringTaskRepository.findAllByActiveTrue())
                    .thenReturn(Collections.singletonList(mockRecurringTask));

            when(taskRepository.findTopByRecurringTaskOrderByAssignedDateDesc(mockRecurringTask))
                    .thenReturn(Optional.of(mockCompleteTask));

            // WHEN:
            recurringTaskSchedulerService.generateTask();

            // THEN:
            verify(taskRepository, times(1)).save(any(Task.class));
            verify(recurringTaskRepository, times(1)).save(mockRecurringTask);
        }

        @Test
        @DisplayName("When no active recurring tasks should do nothing")
        void generateTask_WhenNoActiveTasks_ShouldDoNothing() {
            // GIVEN:
            when(recurringTaskRepository.findAllByActiveTrue())
                    .thenReturn(Collections.emptyList());

            // WHEN:
            recurringTaskSchedulerService.generateTask();

            // THEN:
            verify(taskRepository, never()).save(any());
            verify(recurringTaskRepository, never()).save(any());
        }
    }
}

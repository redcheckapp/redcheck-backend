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

import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
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
        @DisplayName("When today is due but last task is incomplete should NOT create task nor touch the routine")
        void generateTask_WhenLastTaskIncomplete_ShouldNotCreateTaskNorSave() {
            // GIVEN: DAILY is always due, so today is a due day regardless of
            // how stale latestGeneratedDate is
            mockRecurringTask.setLatestGeneratedDate(LocalDateTime.now().minusYears(10));

            when(recurringTaskRepository.findAllByActiveTrue())
                    .thenReturn(Collections.singletonList(mockRecurringTask));

            when(taskRepository.findTopByRecurringTaskAndDeletedFalseOrderByAssignedDateDesc(mockRecurringTask))
                    .thenReturn(Optional.of(mockIncompleteTask));

            // WHEN
            recurringTaskSchedulerService.generateTask();

            // THEN: nothing is generated, and the routine itself is left
            // untouched — latestGeneratedDate must keep reflecting the date
            // of the last actual generation, not a skipped check
            verify(taskRepository, never()).save(any(Task.class));
            verify(recurringTaskRepository, never()).save(any(RecurringTask.class));
        }

        @Test
        @DisplayName("Custom weekday frequency, never generated: should create the first task only on a selected day")
        void generateTask_CustomWeekday_FirstOccurrence_ShouldRespectPattern() {
            // GIVEN: two brand-new routines (latestGeneratedDate null) — one
            // selecting only today's weekday, one selecting only tomorrow's.
            // Before the fix, both generated on the first tick regardless.
            int todayCron = LocalDate.now().getDayOfWeek().getValue() % 7;
            int tomorrowCron = LocalDate.now().plusDays(1).getDayOfWeek().getValue() % 7;
            RecurringTask dueToday = RecurringTask.builder()
                    .title("Due today").frequency("0 0 0 * * " + todayCron).active(true).latestGeneratedDate(null).build();
            RecurringTask dueTomorrow = RecurringTask.builder()
                    .title("Due tomorrow").frequency("0 0 0 * * " + tomorrowCron).active(true).latestGeneratedDate(null).build();

            when(recurringTaskRepository.findAllByActiveTrue()).thenReturn(List.of(dueToday, dueTomorrow));

            // WHEN
            recurringTaskSchedulerService.generateTask();

            // THEN
            verify(taskRepository, times(1)).save(any(Task.class));
            verify(recurringTaskRepository, times(1)).save(dueToday);
            verify(recurringTaskRepository, never()).save(dueTomorrow);
        }

        @Test
        @DisplayName("Custom weekday frequency: should NOT generate on a day that isn't selected, even if overdue")
        void generateTask_CustomWeekday_WhenNotDueToday_ShouldNotGenerate() {
            // GIVEN: a routine that only fires on Mondays (day-of-week 1),
            // whose latestGeneratedDate is a stale Monday from weeks ago —
            // e.g. it was paused and reactivated, or its frequency was
            // edited from a different set of days. Today is whatever day
            // the test actually runs on and is not guaranteed to be a
            // Monday, so pin "today" indirectly by asserting against the
            // real LocalDate.now() below.
            mockRecurringTask.setFrequency("0 0 0 * * 1");
            mockRecurringTask.setLatestGeneratedDate(LocalDate.now().minusYears(1).atStartOfDay());

            when(recurringTaskRepository.findAllByActiveTrue())
                    .thenReturn(Collections.singletonList(mockRecurringTask));

            boolean todayIsMonday = LocalDate.now().getDayOfWeek().getValue() == 1;
            if (todayIsMonday) {
                when(taskRepository.findTopByRecurringTaskAndDeletedFalseOrderByAssignedDateDesc(mockRecurringTask))
                        .thenReturn(Optional.of(mockCompleteTask));
            }

            // WHEN
            recurringTaskSchedulerService.generateTask();

            // THEN: only generates if today genuinely is a Monday; any other
            // day, it must not generate even though the routine is clearly
            // overdue relative to its stale latestGeneratedDate — and the
            // lastTask lookup must never even happen in that case
            if (todayIsMonday) {
                verify(taskRepository, times(1)).save(any(Task.class));
            } else {
                verify(taskRepository, never())
                        .findTopByRecurringTaskAndDeletedFalseOrderByAssignedDateDesc(any(RecurringTask.class));
                verify(taskRepository, never()).save(any(Task.class));
                verify(recurringTaskRepository, never()).save(any(RecurringTask.class));
            }
        }

        @Test
        @DisplayName("Custom weekday frequency: should generate when today matches and the last instance is completed")
        void generateTask_CustomWeekday_WhenDueTodayAndLastCompleted_ShouldGenerate() {
            // GIVEN: frequency set to today's own weekday, with a stale
            // latestGeneratedDate from a year ago so it's clearly "overdue"
            // under the old next-vs-now comparison too
            int todayIsoDayOfWeek = LocalDate.now().getDayOfWeek().getValue();
            int cronDayOfWeek = todayIsoDayOfWeek % 7; // ISO Monday=1..Sunday=7 -> cron Sunday=0..Saturday=6
            mockRecurringTask.setFrequency("0 0 0 * * " + cronDayOfWeek);
            mockRecurringTask.setLatestGeneratedDate(LocalDate.now().minusYears(1).atStartOfDay());

            when(recurringTaskRepository.findAllByActiveTrue())
                    .thenReturn(Collections.singletonList(mockRecurringTask));
            when(taskRepository.findTopByRecurringTaskAndDeletedFalseOrderByAssignedDateDesc(mockRecurringTask))
                    .thenReturn(Optional.of(mockCompleteTask));

            // WHEN
            recurringTaskSchedulerService.generateTask();

            // THEN
            verify(taskRepository, times(1)).save(any(Task.class));
            verify(recurringTaskRepository, times(1)).save(mockRecurringTask);
        }

        @Test
        @DisplayName("Custom weekday frequency: should NOT generate when today matches but the last instance is still incomplete")
        void generateTask_CustomWeekday_WhenDueTodayButLastIncomplete_ShouldNotGenerate() {
            // GIVEN: frequency set to today's own weekday, so it's due, but
            // the previous occurrence hasn't been completed yet
            int todayIsoDayOfWeek = LocalDate.now().getDayOfWeek().getValue();
            int cronDayOfWeek = todayIsoDayOfWeek % 7;
            mockRecurringTask.setFrequency("0 0 0 * * " + cronDayOfWeek);
            mockRecurringTask.setLatestGeneratedDate(LocalDate.now().minusYears(1).atStartOfDay());

            when(recurringTaskRepository.findAllByActiveTrue())
                    .thenReturn(Collections.singletonList(mockRecurringTask));
            when(taskRepository.findTopByRecurringTaskAndDeletedFalseOrderByAssignedDateDesc(mockRecurringTask))
                    .thenReturn(Optional.of(mockIncompleteTask));

            // WHEN
            recurringTaskSchedulerService.generateTask();

            // THEN
            verify(taskRepository, never()).save(any(Task.class));
            verify(recurringTaskRepository, never()).save(any(RecurringTask.class));
        }

        @Test
        @DisplayName("When time passed and last task is completed should create task and update date")
        void generateTask_WhenLastTaskCompleted_ShouldCreateTaskAndUpdateDate() {
            // GIVEN:
            mockRecurringTask.setLatestGeneratedDate(LocalDateTime.now().minusYears(10));

            when(recurringTaskRepository.findAllByActiveTrue())
                    .thenReturn(Collections.singletonList(mockRecurringTask));

            when(taskRepository.findTopByRecurringTaskAndDeletedFalseOrderByAssignedDateDesc(mockRecurringTask))
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

        @Test
        @DisplayName("When one recurring task has an unparseable frequency, the rest of the batch should still be processed")
        void generateTask_WhenOneFrequencyIsUnparseable_ShouldStillProcessTheRest() {
            // GIVEN: the first task's frequency can't be parsed by FrequencyUtils
            // (simulating stale/manually-inserted data, since the DTO validation
            // that normally guards this shouldn't let it happen going forward)
            mockRecurringTask.setFrequency("not a valid frequency");
            mockRecurringTask.setLatestGeneratedDate(LocalDateTime.now().minusYears(10));

            RecurringTask secondRecurringTask = RecurringTask.builder()
                    .id(2L)
                    .title("Review notes")
                    .frequency("DAILY")
                    .active(true)
                    .latestGeneratedDate(null)
                    .build();

            when(recurringTaskRepository.findAllByActiveTrue())
                    .thenReturn(java.util.List.of(mockRecurringTask, secondRecurringTask));

            // WHEN:
            recurringTaskSchedulerService.generateTask();

            // THEN: the broken task is skipped (no save at all for it), but the
            // second, valid task is still generated normally.
            verify(taskRepository, times(1)).save(any(Task.class));
            verify(recurringTaskRepository, never()).save(mockRecurringTask);
            verify(recurringTaskRepository, times(1)).save(secondRecurringTask);
        }

        @Test
        @DisplayName("When a time-of-day is configured, the generated task's deadline should use it")
        void generateTask_WhenTimeIsConfigured_ShouldSetDeadline() {
            // GIVEN
            mockRecurringTask.setLatestGeneratedDate(null);
            mockRecurringTask.setTime(LocalTime.of(18, 0));

            when(recurringTaskRepository.findAllByActiveTrue())
                    .thenReturn(Collections.singletonList(mockRecurringTask));

            ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);

            // WHEN
            recurringTaskSchedulerService.generateTask();

            // THEN
            verify(taskRepository).save(taskCaptor.capture());
            assertEquals(LocalDate.now().atTime(18, 0), taskCaptor.getValue().getDeadline());
        }

        @Test
        @DisplayName("When no time-of-day is configured, the generated task's deadline should stay null")
        void generateTask_WhenNoTimeConfigured_ShouldLeaveDeadlineNull() {
            // GIVEN
            mockRecurringTask.setLatestGeneratedDate(null);

            when(recurringTaskRepository.findAllByActiveTrue())
                    .thenReturn(Collections.singletonList(mockRecurringTask));

            ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);

            // WHEN
            recurringTaskSchedulerService.generateTask();

            // THEN
            verify(taskRepository).save(taskCaptor.capture());
            assertNull(taskCaptor.getValue().getDeadline());
        }

        @Test
        @DisplayName("When today is on or after endDate, should generate the final occurrence and deactivate")
        void generateTask_WhenTodayReachesEndDate_ShouldGenerateAndDeactivate() {
            // GIVEN
            mockRecurringTask.setLatestGeneratedDate(null);
            mockRecurringTask.setEndDate(LocalDate.now());

            when(recurringTaskRepository.findAllByActiveTrue())
                    .thenReturn(Collections.singletonList(mockRecurringTask));

            // WHEN
            recurringTaskSchedulerService.generateTask();

            // THEN
            verify(taskRepository, times(1)).save(any(Task.class));
            assertFalse(mockRecurringTask.isActive());
            verify(recurringTaskRepository, times(1)).save(mockRecurringTask);
        }

        @Test
        @DisplayName("When endDate has already passed, should deactivate without generating")
        void generateTask_WhenEndDateAlreadyPassed_ShouldDeactivateWithoutGenerating() {
            // GIVEN: a run must have been missed, since an active routine
            // shouldn't normally still be active past its own endDate.
            mockRecurringTask.setLatestGeneratedDate(null);
            mockRecurringTask.setEndDate(LocalDate.now().minusDays(1));

            when(recurringTaskRepository.findAllByActiveTrue())
                    .thenReturn(Collections.singletonList(mockRecurringTask));

            // WHEN
            recurringTaskSchedulerService.generateTask();

            // THEN
            verify(taskRepository, never()).save(any(Task.class));
            assertFalse(mockRecurringTask.isActive());
            verify(recurringTaskRepository, times(1)).save(mockRecurringTask);
        }
    }
}
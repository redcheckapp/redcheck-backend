package com.redcheck.backend;

import com.redcheck.backend.dto.response.ProgressRecordResponseDTO;
import com.redcheck.backend.entity.ProgressRecord;
import com.redcheck.backend.entity.User;
import com.redcheck.backend.repository.ProgressRecordRepository;
import com.redcheck.backend.repository.TaskRepository;
import com.redcheck.backend.service.ProgressRecordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unitary Tests - ProgressRecordService")
public class ProgressRecordServiceTest {

    @Mock
    private ProgressRecordRepository progressRecordRepository;

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private ProgressRecordService progressRecordService;

    private User user;
    private ProgressRecord mockProgressRecord;
    private LocalDate testDate;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .username("user")
                .email("user@redcheck.com")
                .password("redcheckUser")
                .build();
        user.setId(1L);

        testDate = LocalDate.of(2023, 10, 15);

        mockProgressRecord = ProgressRecord.builder()
                .user(user)
                .date(testDate)
                .totalTasks(10)
                .completedTasks(5)
                .build();
        mockProgressRecord.setId(100L);
    }

    @Nested
    @DisplayName("Method: getHeatmap")
    class GetHeatmapTests {

        @Test
        @DisplayName("Should return list of mapped progress records for the last year")
        void getHeatmap_ShouldReturnListOfMappedProgressRecords() {
            // GIVEN
            when(progressRecordRepository.findAllByUserAndDateBetweenOrderByDateAsc(eq(user), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(Collections.singletonList(mockProgressRecord));

            // WHEN
            List<ProgressRecordResponseDTO> result = progressRecordService.getHeatmap(user);

            // THEN
            assertFalse(result.isEmpty());
            assertEquals(1, result.size());

            ProgressRecordResponseDTO dto = result.get(0);
            assertEquals(testDate, dto.getDate());
            assertEquals(10, dto.getTotalTasks());
            assertEquals(5, dto.getCompletedTasks());
            assertEquals(0.5, dto.getCompletionRate());
            verify(progressRecordRepository, times(1))
                    .findAllByUserAndDateBetweenOrderByDateAsc(eq(user), any(LocalDate.class), any(LocalDate.class));
        }
    }

    @Nested
    @DisplayName("Method: getDayProgress")
    class GetDayProgressTests {

        @Test
        @DisplayName("When record exists for date should return mapped DTO")
        void getDayProgress_WhenRecordExists_ShouldReturnMappedDTO() {
            // GIVEN
            when(progressRecordRepository.findByUserAndDate(user, testDate))
                    .thenReturn(Optional.of(mockProgressRecord));

            // WHEN
            ProgressRecordResponseDTO result = progressRecordService.getDayProgress(user, testDate);

            // THEN
            assertNotNull(result);
            assertEquals(testDate, result.getDate());
            assertEquals(10, result.getTotalTasks());
            assertEquals(5, result.getCompletedTasks());
            assertEquals(0.5, result.getCompletionRate());
            verify(progressRecordRepository, times(1)).findByUserAndDate(user, testDate);
        }

        @Test
        @DisplayName("When record does not exist for date should return empty DTO")
        void getDayProgress_WhenRecordDoesNotExist_ShouldReturnEmptyDTO() {
            // GIVEN
            when(progressRecordRepository.findByUserAndDate(user, testDate))
                    .thenReturn(Optional.empty());

            // WHEN
            ProgressRecordResponseDTO result = progressRecordService.getDayProgress(user, testDate);

            // THEN
            assertNotNull(result);
            assertEquals(testDate, result.getDate());
            assertEquals(0, result.getTotalTasks());
            assertEquals(0, result.getCompletedTasks());
            assertEquals(0.0, result.getCompletionRate());
            verify(progressRecordRepository, times(1)).findByUserAndDate(user, testDate);
        }
    }

    @Nested
    @DisplayName("Method: generateDailyRecord")
    class GenerateDailyRecordTests {

        @Test
        @DisplayName("When daily record already exists should return without saving")
        void generateDailyRecord_WhenRecordExists_ShouldReturnWithoutSaving() {
            // GIVEN
            when(progressRecordRepository.existsByUserAndDate(eq(user), any(LocalDate.class)))
                    .thenReturn(true);

            // WHEN
            progressRecordService.generateDailyRecord(user);

            // THEN
            verify(progressRecordRepository, times(1)).existsByUserAndDate(eq(user), any(LocalDate.class));

            // Verificamos que no se llama a los nuevos métodos del repositorio
            verify(taskRepository, never()).countBySubjectUserIdAndCompletedDateBetween(anyLong(), any(), any());
            verify(taskRepository, never()).countBySubjectUserIdAndCompletedDateIsNull(anyLong());

            verify(progressRecordRepository, never()).save(any(ProgressRecord.class));
        }

        @Test
        @DisplayName("When daily record does not exist should count tasks and save new record")
        void generateDailyRecord_WhenRecordDoesNotExist_ShouldCountTasksAndSave() {
            // GIVEN
            when(progressRecordRepository.existsByUserAndDate(eq(user), any(LocalDate.class)))
                    .thenReturn(false);

            when(taskRepository.countBySubjectUserIdAndCompletedDateBetween(
                    eq(user.getId()), any(LocalDateTime.class), any(LocalDateTime.class)))
                    .thenReturn(2L);

            when(taskRepository.countBySubjectUserIdAndCompletedDateIsNull(user.getId()))
                    .thenReturn(4L);

            // WHEN
            progressRecordService.generateDailyRecord(user);

            // THEN
            verify(progressRecordRepository, times(1)).existsByUserAndDate(eq(user), any(LocalDate.class));
            verify(taskRepository, times(1)).countBySubjectUserIdAndCompletedDateBetween(
                    eq(user.getId()), any(LocalDateTime.class), any(LocalDateTime.class));
            verify(taskRepository, times(1)).countBySubjectUserIdAndCompletedDateIsNull(user.getId());

            ArgumentCaptor<ProgressRecord> recordCaptor = ArgumentCaptor.forClass(ProgressRecord.class);
            verify(progressRecordRepository, times(1)).save(recordCaptor.capture());

            ProgressRecord savedRecord = recordCaptor.getValue();

            assertEquals(6, savedRecord.getTotalTasks());
            assertEquals(2, savedRecord.getCompletedTasks());
            assertNotNull(savedRecord.getDate());
            assertEquals(user, savedRecord.getUser());
        }
    }
}
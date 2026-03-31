package com.redcheck.backend;

import com.redcheck.backend.entity.User;
import com.redcheck.backend.repository.UserRepository;
import com.redcheck.backend.service.ProgressRecordSchedulerService;
import com.redcheck.backend.service.ProgressRecordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unitary Tests - ProgressRecordSchedulerService")
public class ProgressRecordSchedulerServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProgressRecordService progressRecordService;

    @InjectMocks
    private ProgressRecordSchedulerService progressRecordSchedulerService;

    private User user1;
    private User user2;

    @BeforeEach
    void setUp() {
        user1 = User.builder()
                .username("user1")
                .email("user1@redcheck.com")
                .password("password")
                .build();
        user1.setId(1L);

        user2 = User.builder()
                .username("user2")
                .email("user2@redcheck.com")
                .password("password")
                .build();
        user2.setId(2L);
    }

    @Nested
    @DisplayName("Method: generateDailyProgressRecords")
    class GenerateDailyProgressRecordsTests {

        @Test
        @DisplayName("When users exist should call generateDailyRecord for each user")
        void generateDailyProgressRecords_WhenUsersExist_ShouldCallServiceForEachUser() {
            // GIVEN
            List<User> users = Arrays.asList(user1, user2);
            when(userRepository.findAll()).thenReturn(users);

            // WHEN
            progressRecordSchedulerService.generateDailyProgressRecords();

            // THEN
            verify(userRepository, times(1)).findAll();
            verify(progressRecordService, times(2)).generateDailyRecord(any(User.class));
        }

        @Test
        @DisplayName("When no users exist should not call generateDailyRecord")
        void generateDailyProgressRecords_WhenNoUsersExist_ShouldNotCallService() {
            // GIVEN
            when(userRepository.findAll()).thenReturn(Collections.emptyList());

            // WHEN
            progressRecordSchedulerService.generateDailyProgressRecords();

            // THEN
            verify(userRepository, times(1)).findAll();
            verify(progressRecordService, never()).generateDailyRecord(any(User.class));
        }
    }
}
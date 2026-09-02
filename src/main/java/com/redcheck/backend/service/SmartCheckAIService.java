package com.redcheck.backend.service;

import com.redcheck.backend.dto.request.EngineRequestDTO;
import com.redcheck.backend.entity.AiResponse;
import com.redcheck.backend.entity.Notification;
import com.redcheck.backend.entity.Task;
import com.redcheck.backend.entity.User;
import com.redcheck.backend.repository.AiResponseRepository;
import com.redcheck.backend.repository.NotificationRepository;
import com.redcheck.backend.repository.TaskRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmartCheckAIService {

    private final NotificationRepository notificationRepository;
    private final TaskRepository taskRepository;
    private final AiResponseRepository aiResponseRepository;

    @Value("${ai.engine.url}")
    private String aiEngineUrl;

    public String getTodaysAnalysis(User currentUser) {
        return aiResponseRepository
                .findFirstByUserAndTypeOrderByCreatedDateDesc(
                        currentUser,
                        AiResponse.Type.DAILY_ANALYSIS)
                .filter(aiResponse -> aiResponse.getCreatedDate().toLocalDate().equals(LocalDate.now()))
                .map(AiResponse::getPayload)
                .orElse(null);
    }

    @Transactional
    public void deleteTodaysAnalysis(User currentUser) {
        aiResponseRepository
                .findFirstByUserAndTypeOrderByCreatedDateDesc(
                        currentUser,
                        AiResponse.Type.DAILY_ANALYSIS)
                .ifPresent(aiResponseRepository::delete);
    }

    @Async
    @Transactional
    public void runDailySmartAnalysis(User currentUser, String lang) {
        try {
            List<Task> pendingTasks = taskRepository.findAllBySubject_User_IdAndCompletedDateIsNullAndDeletedFalse(currentUser.getId());

            List<Map<String, Object>> simplifiedTasks = pendingTasks.stream()
                    .map(task -> {
                        Map<String, Object> taskMap = new HashMap<>();
                        taskMap.put("id", task.getId());
                        taskMap.put("titulo", task.getTitle());
                        taskMap.put("descripcion", task.getDescription());
                        taskMap.put("fechaLimite", task.getDeadline() != null ? task.getDeadline().toString() : null);

                        if (task.getSubject() != null) {
                            taskMap.put("asignatura", task.getSubject().getName());
                        }
                        return taskMap;
                    })
                    .collect(Collectors.toList());

            // 1. Retrieve aggregated metrics from MySQL
            List<Object[]> queryResults = taskRepository.getSubjectCompletionRatios(currentUser.getId());
            Map<String, Integer> userAnalytics = new HashMap<>();

            for (Object[] result : queryResults) {
                String subjectName = (String) result[0];
                Integer ratio = ((Number) result[1]).intValue();
                userAnalytics.put(subjectName, ratio);
            }

            // 2. Assemble the typed DTO
            EngineRequestDTO requestPayload = new EngineRequestDTO(
                    currentUser.getId().toString(),
                    lang,
                    userAnalytics,
                    simplifiedTasks
            );

            // 3. Instantiate RestTemplate and transmit the payload
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<EngineRequestDTO> entity = new HttpEntity<>(requestPayload, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    aiEngineUrl + "/api/v1/prioritize",
                    entity,
                    String.class
            );

            // 4. Save the returned JSON
            AiResponse aiResponse = AiResponse.builder()
                    .type(AiResponse.Type.DAILY_ANALYSIS)
                    .payload(response.getBody())
                    .user(currentUser)
                    .build();
            aiResponseRepository.save(aiResponse);

            Notification notification = Notification.builder()
                    .title("Analysis completed")
                    .message("Tap to view your SmartCheck AI analysis")
                    .user(currentUser)
                    .build();
            notificationRepository.save(notification);

        } catch (Exception e) {
            log.error("Error during SmartCheck AI analysis for user ID {}", currentUser.getId(), e);
            Notification notificationError = Notification.builder()
                    .title("SmartCheck AI Error")
                    .message("We couldn't analyze your tasks.")
                    .user(currentUser)
                    .build();
            notificationRepository.save(notificationError);
        }
    }
}
package com.redcheck.backend.service;

import com.redcheck.backend.entity.AiResponse;
import com.redcheck.backend.entity.Notification;
import com.redcheck.backend.entity.Task;
import com.redcheck.backend.entity.User;
import com.redcheck.backend.repository.AiResponseRepository;
import com.redcheck.backend.repository.NotificationRepository;
import com.redcheck.backend.repository.TaskRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
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

@Service
@RequiredArgsConstructor
public class SmartCheckAIService {

    private final NotificationRepository notificationRepository;
    private final TaskRepository taskRepository;
    private final AiResponseRepository aiResponseRepository;

    @Value("${ai.engine.url:http://localhost:8000}")
    private String aiEngineUrl;

    public String getTodaysAnalysis(User currentUser){
        return aiResponseRepository
                .findFirstByUserAndTypeOrderByCreatedDateDesc(
                        currentUser,
                        AiResponse.Type.DAILY_ANALYSIS)
                .filter(aiResponse -> aiResponse.getCreatedDate().toLocalDate().equals(LocalDate.now()))
                .map(AiResponse::getPayload)
                .orElse(null);
    }

    @Transactional
    public void deleteTodaysAnalysis(User currentUser){
        aiResponseRepository
                .findFirstByUserAndTypeOrderByCreatedDateDesc(
                        currentUser,
                        AiResponse.Type.DAILY_ANALYSIS)
                .ifPresent(aiResponseRepository::delete);
    }

    @Async
    @Transactional
    public void runDailySmartAnalysis(User currentUser, String lang){
        try {
            List<Task> pendingTasks = taskRepository.findAllBySubject_User_IdAndCompletedDateIsNullAndDeletedFalse(currentUser.getId());

            List<Map<String, Object>> simplifiedTasks = pendingTasks.stream()
                    .map(task -> {
                        Map<String, Object> taskMap = new HashMap<>();
                        taskMap.put("id", task.getId());
                        taskMap.put("titulo", task.getTitle());
                        taskMap.put("descripcion", task.getDescription());
                        taskMap.put("fechaLimite", task.getDeadline() != null ? task.getDeadline().toString() : null);

                        if(task.getSubject() != null){
                            taskMap.put("asignatura", task.getSubject().getName());
                        }
                        return taskMap;
                    })
                    .collect(Collectors.toList());

            // TODO: Conectar con ProgressRecordService para obtener analíticas reales del usuario
            Map<String, Integer> userAnalytics = new HashMap<>();

            // Perfil técnico inyectado para el cálculo de esfuerzo cognitivo en Python
            String userProfile = "Desarrollador de software enfocado en backend con Java, Spring Boot y React. Delega la escritura de código casi por completo a la IA, priorizando la arquitectura, la seguridad y la orquestación de contenedores. Tareas de configuración de infraestructura tienen alta carga cognitiva.";

            Map<String, Object> requestPayload = new HashMap<>();
            requestPayload.put("userId", currentUser.getId().toString());
            requestPayload.put("userProfile", userProfile);
            requestPayload.put("userAnalytics", userAnalytics);
            requestPayload.put("tasks", simplifiedTasks);

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestPayload, headers);

            // Llamada interna al microservicio de Python
            ResponseEntity<String> response = restTemplate.postForEntity(
                    aiEngineUrl + "/api/v1/prioritize",
                    entity,
                    String.class
            );

            // Guardamos directamente el JSON devuelto por Pydantic
            AiResponse aiResponse = AiResponse.builder()
                    .type(AiResponse.Type.DAILY_ANALYSIS)
                    .payload(response.getBody())
                    .user(currentUser)
                    .build();
            aiResponseRepository.save(aiResponse);

            Notification notification = Notification.builder()
                    .title("Análisis completado")
                    .message("Toca para ver el análisis de SmartCheck AI")
                    .user(currentUser)
                    .build();
            notificationRepository.save(notification);

        } catch (Exception e) {
            e.printStackTrace();
            Notification notificationError = Notification.builder()
                    .title("Error en SmartCheck AI")
                    .message("No pudimos analizar tus tareas.")
                    .user(currentUser)
                    .build();
            notificationRepository.save(notificationError);
        }
    }
}
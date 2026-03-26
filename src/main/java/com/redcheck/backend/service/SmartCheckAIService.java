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
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SmartCheckAIService {

    private final NotificationRepository notificationRepository;
    private final TaskRepository taskRepository;
    private final AiResponseRepository aiResponseRepository;
    private final OllamaService ollamaService;

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
    public void runDailySmartAnalysis(User currentUser){
        try{

            // Look for this user's uncompleted tasks
            List<Task> pendingTasks = taskRepository.findAllBySubject_User_IdAndCompletedDateIsNull(currentUser.getId());

            ObjectMapper objectMapper = new ObjectMapper();

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

            String jsonStringTasks = objectMapper.writeValueAsString(simplifiedTasks);

            // Build the prompt
            String prompt =
                    "Eres SmartCheck AI, un asistente experto en productividad para estudiantes.\n" +
                    "La fecha de hoy es: " + LocalDateTime.now()  + ". \n" +
                    "\n" +
                    "El usuario tiene las siguientes tareas pendientes en formato JSON: \n" +
                    jsonStringTasks + "\n" +
                    "\n" +
                    "Analiza su carga de trabajo y créale un plan de estudio realista SOLO PARA HOY. \n" +
                    "Devuelve tu respuesta ÚNICA Y EXCLUSIVAMENTE en el siguiente formato JSON válido, sin texto adicional ni markdown:\n" +
                    "{\n" +
                    "  \"mensajeApoyo\": \"Un mensaje breve analizando cómo se presenta el día y dándole una estrategia general de ataque.\",\n" +
                    "  \"nivelRiesgo\": \"ALTO\", // Evalúa si la carga de hoy es excesiva (ALTO, MEDIO, BAJO)\n" +
                    "  \"planDeHoy\": [\n" +
                    "    {\n" +
                    "      \"id\": 1,\n" +
                    "      \"ordenDefinido\": 1,\n" +
                    "      \"razonPrioridad\": \"Haz esto primero porque está fuera de plazo.\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"id\": 4,\n" +
                    "      \"ordenDefinido\": 2,\n" +
                    "      \"razonPrioridad\": \"Sigue con esto porque vence hoy a las 23:59.\"\n" +
                    "    }\n" +
                    "  ]\n" +
                    "}";

            String aiResponseJson = ollamaService.askLlama(prompt);
            AiResponse aiResponse = AiResponse.builder()
                    .type(AiResponse.Type.DAILY_ANALYSIS)
                    .payload(aiResponseJson)
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
            Notification notificationError = Notification.builder()
                    .title("Error en SmartCheck AI")
                    .message("No pudimos analizar tus tareas.")
                    .user(currentUser)
                    .build();
            notificationRepository.save(notificationError);
        }
    }
}

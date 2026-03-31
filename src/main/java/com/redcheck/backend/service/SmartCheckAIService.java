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
    private final AIService aiService;

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

            String prompt =
                            "Actúa como SmartCheck AI, un experto analista de productividad y gestión del tiempo para estudiantes.\n" +
                            "Tu objetivo es crear un plan de ataque estratégico, realista y priorizado SOLO PARA HOY, basándote en datos objetivos.\n\n" +
                            "FECHA Y HORA ACTUAL: " + LocalDateTime.now() + "\n\n" +
                            "REGLAS ESTRICTAS DE PRIORIZACIÓN QUE DEBES SEGUIR OBLIGATORIAMENTE:\n" +
                            "0. COBERTURA TOTAL: Analiza TODAS las asignaturas y tareas proporcionadas. Tu plan final DEBE incluir TODAS las tareas pendientes del JSON de entrada, ordenadas según las siguientes reglas. No omitas ninguna.\n" +
                            "1. URGENCIA CRÍTICA: Las tareas atrasadas ('overdue') o con fecha límite anterior a la actual van siempre en los primeros lugares (ordenDefinido 1, 2...).\n" +
                            "2. VENCIMIENTO HOY: Las tareas cuya fecha límite es hoy van justo después de las atrasadas.\n" +
                            "3. PLANIFICACIÓN Y ADELANTO: Las tareas sin fecha límite o para días futuros deben ir al final del plan (con los últimos números de orden) para indicar que se deben hacer si sobra tiempo.\n\n" +
                            "REGLAS PARA EL 'nivelRiesgo':\n" +
                            "- 'ALTO': Si hay 3 o más tareas atrasadas o que vencen hoy. El mensaje de apoyo debe ser empático, pidiendo foco total para apagar incendios.\n" +
                            "- 'MEDIO': Si hay 1 o 2 tareas atrasadas o que vencen hoy. El mensaje debe animar a quitárselas de en medio rápido.\n" +
                            "- 'BAJO': Si todo está al día o no hay tareas urgentes. El mensaje debe motivar a adelantar trabajo relajadamente.\n\n" +
                            "TAREAS PENDIENTES DEL USUARIO (JSON):\n" +
                            jsonStringTasks + "\n\n" +
                            "INSTRUCCIONES DE FORMATO DE SALIDA:\n" +
                            "Devuelve tu respuesta ÚNICA Y EXCLUSIVAMENTE en formato JSON puro. Genera un objeto dentro del array 'planDeHoy' por CADA tarea recibida en la entrada. Sigue estrictamente esta estructura:\n" +
                            "{\n" +
                            "  \"mensajeApoyo\": \"[Mensaje breve y directo analizando el día]\",\n" +
                            "  \"nivelRiesgo\": \"[ALTO, MEDIO o BAJO]\",\n" +
                            "  \"planDeHoy\": [\n" +
                            "    {\n" +
                            "      \"id\": [ID numérico de la tarea],\n" +
                            "      \"ordenDefinido\": [1, 2, 3...],\n" +
                            "      \"razonPrioridad\": \"[Justificación breve según las reglas 1, 2 o 3]\"\n" +
                            "    }\n" +
                            "    // ... REPITE ESTE OBJETO PARA TODAS LAS TAREAS DE LA LISTA, ASEGURANDO COBERTURA TOTAL.\n" +
                            "  ]\n" +
                            "}";

            String aiResponseJson = aiService.ask(prompt);
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

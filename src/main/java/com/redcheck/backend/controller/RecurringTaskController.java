package com.redcheck.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/subjects/{subjectId}/recurring-tasks")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RecurringTaskController {

    /*
    TODO:   GET    /subjects/{subjectId}/recurring-tasks              → todas las tareas recurrentes del usuario (con filtro ?active=)
            GET    /subjects/{subjectId}/recurring-tasks/{id}         → una sola tarea recurrente
            POST   /subjects/{subjectId}/recurring-tasks              → crear tarea recurrente
            PUT    /subjects/{subjectId}/recurring-tasks/{id}         → actualizar tarea recurrente
            DELETE /subjects/{subjectId}/recurring-tasks/{id}         → borrar tarea recurrente
            PATCH  /subjects/{subjectId}/recurring-tasks/{id}/active  → activar/desactivar
            (IMPROBABLE) POST   /subjects/{subjectId}/recurring-tasks/{id}/generate → forzar generación de tarea
    */
}

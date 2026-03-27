package com.redcheck.backend.service;

import com.redcheck.backend.dto.update.TaskCompleteDTO;
import com.redcheck.backend.dto.request.TaskRequestDTO;
import com.redcheck.backend.dto.response.TaskResponseDTO;
import com.redcheck.backend.entity.Subject;
import com.redcheck.backend.entity.Task;
import com.redcheck.backend.entity.User;
import com.redcheck.backend.exception.SubjectNotFoundException;
import com.redcheck.backend.exception.SubjectNotOwnedException;
import com.redcheck.backend.exception.TaskNotFoundException;
import com.redcheck.backend.exception.TaskNotOwnedException;
import com.redcheck.backend.repository.SubjectRepository;
import com.redcheck.backend.repository.TaskRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final SubjectRepository subjectRepository;

    public List<TaskResponseDTO> getAllTask(User currentUser, Long subjectId, Boolean completed, Boolean overdue) {

        List<Task> rawTasks;

        if (overdue != null && overdue){
            rawTasks = taskRepository.findAllBySubject_User_IdAndDeadlineBeforeAndCompletedDateIsNull(
                    currentUser.getId(), LocalDateTime.now());
        } else if (completed != null && completed){
            rawTasks = taskRepository.findAllBySubject_User_IdAndCompletedDateIsNotNull(currentUser.getId());
        }else if (completed != null && !completed) {
            LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
            LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

            rawTasks = taskRepository.findPendingOrCompletedToday(
                    currentUser.getId(), startOfDay, endOfDay);
        }else {
            rawTasks = taskRepository.findAllBySubject_User_Id(currentUser.getId());
        }

        // At this moment, only subjectId remains in memory, a simple field
        return rawTasks.stream()
                .map(this::toResponseDTO)
                .filter(task -> subjectId == null || task.getSubjectId().equals(subjectId))
                .collect(Collectors.toList());
    }

    @Transactional
    public TaskResponseDTO createTask(Long subjectId, TaskRequestDTO requestDTO, User currentUser){

        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new SubjectNotFoundException(subjectId));

        if(!subject.getUser().getId().equals(currentUser.getId()))
            throw new SubjectNotOwnedException();

        Task task = Task.builder()
                .title(requestDTO.getTitle())
                .description(requestDTO.getDescription())
                .deadline(requestDTO.getDeadline())
                .subject(subject)
                .build();

        taskRepository.save(task);
        return toResponseDTO(task);
    }

    @Transactional
    public TaskResponseDTO updateTask(Long subjectId, Long taskId, TaskRequestDTO requestDTO, User currentUser){

        Task task = getOwnedTask(subjectId, taskId, currentUser);

        task.setTitle(requestDTO.getTitle());
        task.setDescription(requestDTO.getDescription());
        task.setDeadline(requestDTO.getDeadline());

        taskRepository.save(task);
        return toResponseDTO(task);
    }

    @Transactional
    public void deleteTask(Long subjectId, Long taskId, User currentUser){

        Task task = getOwnedTask(subjectId, taskId, currentUser);
        taskRepository.delete(task);
    }

    @Transactional
    public TaskResponseDTO markTaskAsCompleted(Long subjectId, Long taskId, TaskCompleteDTO requestDTO, User currentUser){

        Task task = getOwnedTask(subjectId, taskId, currentUser);

        if(requestDTO.isCompleted())
            task.setCompletedDate(LocalDateTime.now());
        else
            task.setCompletedDate(null);

        taskRepository.save(task);
        return toResponseDTO(task);
    }

    // --- Auxiliary methods ---

    private Task getOwnedTask(Long subjectId, Long taskId, User currentUser){
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new SubjectNotFoundException(subjectId));

        if(!subject.getUser().getId().equals(currentUser.getId()))
            throw new SubjectNotOwnedException();

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));

        if(!task.getSubject().getId().equals(subjectId))
            throw new TaskNotOwnedException();

        return task;
    }

    private TaskResponseDTO toResponseDTO(Task task){
        // Calculates if its overdue based on server hour
        boolean isOverdue = task.getDeadline() != null
                            && task.getDeadline().isBefore(LocalDateTime.now())
                            && task.getCompletedDate() == null;

        return TaskResponseDTO.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .assignedDate(task.getAssignedDate())
                .deadline(task.getDeadline())
                .completedDate(task.getCompletedDate())
                .completed(task.getCompletedDate() != null)
                .overdue(isOverdue)
                .subjectId(task.getSubject().getId())
                .build();
    }

}

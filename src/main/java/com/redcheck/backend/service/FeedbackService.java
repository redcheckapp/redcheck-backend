package com.redcheck.backend.service;

import com.redcheck.backend.dto.request.FeedbackReplyRequestDTO;
import com.redcheck.backend.dto.request.FeedbackRequestDTO;
import com.redcheck.backend.dto.response.AdminFeedbackResponseDTO;
import com.redcheck.backend.dto.response.FeedbackResponseDTO;
import com.redcheck.backend.dto.update.AdminFeedbackUpdateDTO;
import com.redcheck.backend.entity.Feedback;
import com.redcheck.backend.entity.Notification;
import com.redcheck.backend.entity.User;
import com.redcheck.backend.exception.FeedbackAuthorDeletedException;
import com.redcheck.backend.exception.FeedbackNotFoundException;
import com.redcheck.backend.repository.FeedbackRepository;
import com.redcheck.backend.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final NotificationRepository notificationRepository;
    private final EmailService emailService;
    private final AdminAccessService adminAccessService;

    @Transactional
    public FeedbackResponseDTO createFeedback(FeedbackRequestDTO requestDTO, User currentUser) {
        Feedback feedback = Feedback.builder()
                .category(requestDTO.category())
                .message(requestDTO.message())
                .user(currentUser)
                .build();

        feedbackRepository.save(feedback);

        // @Async — plain values only, never the entity, since this runs on
        // another thread after this transaction is gone.
        emailService.sendNewFeedbackNotification(
                adminAccessService.getAdminEmails(),
                feedback.getCategory().name(),
                feedback.getMessage(),
                currentUser != null ? currentUser.getEmail() : null);

        return toResponseDTO(feedback);
    }

    // Developer dashboard only (GET /admin/feedback). Newest first; filtering
    // by category/status happens client-side, the volume is tiny.
    @Transactional(readOnly = true)
    public List<AdminFeedbackResponseDTO> getAllFeedbackForAdmin() {
        return feedbackRepository.findAllByOrderByCreatedDateDesc().stream()
                .map(this::toAdminResponseDTO)
                .toList();
    }

    // Developer dashboard only (PATCH /admin/feedback/{id}).
    @Transactional
    public AdminFeedbackResponseDTO updateFeedbackForAdmin(Long id, AdminFeedbackUpdateDTO updateDTO) {
        Feedback feedback = feedbackRepository.findById(id)
                .orElseThrow(() -> new FeedbackNotFoundException(id));

        if (updateDTO.status() != null) {
            feedback.setStatus(updateDTO.status());
        }
        if (updateDTO.adminNote() != null) {
            feedback.setAdminNote(updateDTO.adminNote().isBlank() ? null : updateDTO.adminNote());
        }

        feedbackRepository.save(feedback);
        return toAdminResponseDTO(feedback);
    }

    // Developer dashboard only (POST /admin/feedback/{id}/notify): sends the
    // author an in-app notification, and optionally the same text by email (FEEDBACK_REPLY, shown in the frontend's
    // bell). The title is a fixed marker — the frontend renders its own
    // localized heading for this type, only the admin's message is shown.
    @Transactional
    public AdminFeedbackResponseDTO notifyAuthor(Long id, FeedbackReplyRequestDTO requestDTO) {
        Feedback feedback = feedbackRepository.findById(id)
                .orElseThrow(() -> new FeedbackNotFoundException(id));
        if (feedback.getUser() == null) {
            throw new FeedbackAuthorDeletedException(id);
        }

        notificationRepository.save(Notification.builder()
                .type(Notification.Type.FEEDBACK_REPLY)
                .title("Feedback reply")
                .message(requestDTO.message().trim())
                .user(feedback.getUser())
                .build());

        // @Async — plain values only, same as the new-feedback email.
        if (Boolean.TRUE.equals(requestDTO.sendEmail())) {
            emailService.sendFeedbackReplyEmail(
                    feedback.getUser().getEmail(),
                    requestDTO.message().trim(),
                    feedback.getMessage(),
                    requestDTO.lang());
        }

        feedback.setUserNotifiedDate(LocalDateTime.now());
        feedbackRepository.save(feedback);
        return toAdminResponseDTO(feedback);
    }

    private FeedbackResponseDTO toResponseDTO(Feedback feedback) {
        return FeedbackResponseDTO.builder()
                .id(feedback.getId())
                .category(feedback.getCategory())
                .message(feedback.getMessage())
                .createdDate(feedback.getCreatedDate())
                .build();
    }

    private AdminFeedbackResponseDTO toAdminResponseDTO(Feedback feedback) {
        User author = feedback.getUser();
        return AdminFeedbackResponseDTO.builder()
                .id(feedback.getId())
                .category(feedback.getCategory())
                .status(feedback.getStatus())
                .message(feedback.getMessage())
                .adminNote(feedback.getAdminNote())
                .createdDate(feedback.getCreatedDate())
                .userNotifiedDate(feedback.getUserNotifiedDate())
                .userEmail(author != null ? author.getEmail() : null)
                .userAlias(author != null ? author.getDisplayAlias() : null)
                .build();
    }
}

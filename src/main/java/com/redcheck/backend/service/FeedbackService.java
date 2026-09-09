package com.redcheck.backend.service;

import com.redcheck.backend.dto.request.FeedbackRequestDTO;
import com.redcheck.backend.dto.response.FeedbackResponseDTO;
import com.redcheck.backend.entity.Feedback;
import com.redcheck.backend.entity.User;
import com.redcheck.backend.repository.FeedbackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;

    @Transactional
    public FeedbackResponseDTO createFeedback(FeedbackRequestDTO requestDTO, User currentUser) {
        Feedback feedback = Feedback.builder()
                .category(requestDTO.category())
                .message(requestDTO.message())
                .user(currentUser)
                .build();

        feedbackRepository.save(feedback);
        return toResponseDTO(feedback);
    }

    private FeedbackResponseDTO toResponseDTO(Feedback feedback) {
        return FeedbackResponseDTO.builder()
                .id(feedback.getId())
                .category(feedback.getCategory())
                .message(feedback.getMessage())
                .createdDate(feedback.getCreatedDate())
                .build();
    }
}

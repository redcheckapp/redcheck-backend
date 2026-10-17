package com.redcheck.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

// In-app reply to a feedback's author. 255 = Notification.message's column
// size (a plain VARCHAR, never widened). sendEmail additionally emails the
// same message from the no-reply address; lang picks that email's template
// language (the author's own language isn't stored anywhere).
@Builder
public record FeedbackReplyRequestDTO(
        @NotBlank(message = "Message is required")
        @Size(max = 255, message = "Message must not exceed 255 characters")
        String message,

        // Optional on the wire (null = false): Jackson 3 rejects a missing
        // primitive, so a client that omits it would otherwise get a 400.
        Boolean sendEmail,

        String lang
) {}

package com.redcheck.backend.service;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String from;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    // Fire-and-forget, same reasoning as SmartCheckAIService's async AI
    // call: the caller (PasswordResetService#forgotPassword) must always
    // return its generic response quickly and regardless of whether sending
    // actually succeeds, so any failure here is logged, not propagated.
    @Async
    public void sendPasswordResetEmail(String to, String resetLink, String lang) {
        boolean spanish = !"en".equalsIgnoreCase(lang);
        String subject = spanish ? "Restablece tu contraseña de RedCheck" : "Reset your RedCheck password";

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "utf-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(buildHtml(resetLink, spanish), true);
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send password reset email to {}", to, e);
        }
    }

    // Deliberately minimal: one accent color (the brand red), generous
    // whitespace, a single CTA button — no logo/imagery, to match the
    // frontend's own clean/minimal visual language (see redcheck-frontend's
    // CLAUDE.md "Styling" note) rather than a heavier marketing-style email.
    private String buildHtml(String resetLink, boolean spanish) {
        String heading = spanish ? "Restablece tu contraseña" : "Reset your password";
        String body = spanish
                ? "Hemos recibido una solicitud para restablecer la contraseña de tu cuenta de RedCheck. Este enlace caduca en 30 minutos."
                : "We received a request to reset your RedCheck account's password. This link expires in 30 minutes.";
        String button = spanish ? "Restablecer contraseña" : "Reset password";
        String ignore = spanish
                ? "Si no has solicitado esto, puedes ignorar este correo con tranquilidad."
                : "If you didn't request this, you can safely ignore this email.";

        return """
                <div style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; max-width: 480px; margin: 0 auto; padding: 40px 24px; color: #27272a;">
                    <h1 style="font-size: 20px; font-weight: 700; margin: 0 0 16px;">%s</h1>
                    <p style="font-size: 14px; line-height: 1.6; color: #52525b; margin: 0 0 28px;">%s</p>
                    <a href="%s" style="display: inline-block; background-color: #dc2626; color: #ffffff; text-decoration: none; font-size: 14px; font-weight: 600; padding: 12px 24px; border-radius: 12px;">%s</a>
                    <p style="font-size: 12px; line-height: 1.6; color: #a1a1aa; margin: 32px 0 0;">%s</p>
                </div>
                """.formatted(heading, body, resetLink, button, ignore);
    }
}

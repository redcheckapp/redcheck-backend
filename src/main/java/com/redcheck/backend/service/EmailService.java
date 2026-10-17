package com.redcheck.backend.service;

import com.redcheck.backend.dto.request.ResendEmailRequestDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.HtmlUtils;

import java.util.Collection;

@Slf4j
@Service
public class EmailService {

    private static final String RESEND_API_URL = "https://api.resend.com/emails";

    @Value("${app.mail.from}")
    private String from;

    @Value("${app.resend.api-key}")
    private String resendApiKey;

    // Reused to build the logo's public URL below (the frontend serves
    // public/icons/icon-192.png as a static asset) — not for the reset
    // link itself, which PasswordResetService already builds and passes in.
    @Value("${app.frontend.url}")
    private String frontendUrl;

    // Sent over Resend's HTTPS API (port 443), not SMTP. Production's VPS
    // provider silently black-holes all outbound SMTP traffic — confirmed
    // with a raw `openssl s_client` test against ports 587/2587 (STARTTLS)
    // and 465/2465 (implicit TLS): the TCP handshake completes but the
    // remote never sends the SMTP banner, on every port, regardless of
    // provider (this almost certainly would have broken Brevo too — it was
    // never actually tested from this server, only from a local machine).
    // HTTPS is unaffected (the app already calls Gemini/the AI engine over
    // it), so routing through Resend's REST API instead of
    // spring-boot-starter-mail/JavaMailSender sidesteps the whole problem
    // rather than chasing ports. Fire-and-forget, same reasoning as
    // SmartCheckAIService's async external calls: the caller
    // (PasswordResetService#forgotPassword) must always return its generic
    // response quickly and regardless of whether sending actually
    // succeeds, so any failure here is logged, not propagated.
    @Async
    public void sendPasswordResetEmail(String to, String resetLink, String lang) {
        boolean spanish = !"en".equalsIgnoreCase(lang);
        String subject = spanish ? "Restablece tu contraseña de RedCheck" : "Reset your RedCheck password";

        log.info("Sending password reset email to {} (from={})", to, from);
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(resendApiKey);

            ResendEmailRequestDTO requestPayload = ResendEmailRequestDTO.builder()
                    .from(from)
                    .to(to)
                    .subject(subject)
                    .html(buildHtml(resetLink, spanish))
                    .build();

            restTemplate.postForEntity(RESEND_API_URL, new HttpEntity<>(requestPayload, headers), String.class);
            log.info("Password reset email accepted by Resend for {}", to);
        } catch (Exception e) {
            log.error("Failed to send password reset email to {}", to, e);
        }
    }

    // Heads-up to the developer(s) in ADMIN_EMAILS whenever someone submits
    // feedback, so the dashboard doesn't have to be polled. Spanish-only on
    // purpose: the only recipient is the developer, not end users. Same
    // fire-and-forget contract as sendPasswordResetEmail — POST /feedback must
    // succeed regardless of whether this does.
    @Async
    public void sendNewFeedbackNotification(Collection<String> recipients, String category,
                                            String message, String authorEmail) {
        if (recipients.isEmpty()) {
            return;
        }

        // The message is user-supplied, so it's escaped before going into the
        // HTML body, and newlines are stripped from the subject snippet.
        String snippet = message.replaceAll("\\s+", " ").trim();
        if (snippet.length() > 60) {
            snippet = snippet.substring(0, 60) + "…";
        }
        String subject = "[RedCheck] " + category + ": " + snippet;
        String html = buildFeedbackHtml(category, message, authorEmail);

        for (String to : recipients) {
            try {
                RestTemplate restTemplate = new RestTemplate();
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setBearerAuth(resendApiKey);

                ResendEmailRequestDTO requestPayload = ResendEmailRequestDTO.builder()
                        .from(from)
                        .to(to)
                        .subject(subject)
                        .html(html)
                        .build();

                restTemplate.postForEntity(RESEND_API_URL, new HttpEntity<>(requestPayload, headers), String.class);
                log.info("New feedback notification accepted by Resend for {}", to);
            } catch (Exception e) {
                log.error("Failed to send new feedback notification to {}", to, e);
            }
        }
    }

    // Optional copy of a dashboard reply (POST /admin/feedback/{id}/notify
    // with sendEmail=true) to the feedback's author. Sent from MAIL_FROM,
    // which is the no-reply address — the template says so, since a reply to
    // it goes nowhere. Same fire-and-forget contract as the other emails.
    @Async
    public void sendFeedbackReplyEmail(String to, String reply, String originalMessage, String lang) {
        boolean spanish = !"en".equalsIgnoreCase(lang);
        String subject = spanish ? "Respuesta a tu feedback en RedCheck" : "A reply to your RedCheck feedback";

        log.info("Sending feedback reply email to {}", to);
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(resendApiKey);

            ResendEmailRequestDTO requestPayload = ResendEmailRequestDTO.builder()
                    .from(from)
                    .to(to)
                    .subject(subject)
                    .html(buildFeedbackReplyHtml(reply, originalMessage, spanish))
                    .build();

            restTemplate.postForEntity(RESEND_API_URL, new HttpEntity<>(requestPayload, headers), String.class);
            log.info("Feedback reply email accepted by Resend for {}", to);
        } catch (Exception e) {
            log.error("Failed to send feedback reply email to {}", to, e);
        }
    }

    // Brand lockup + the reply, with the user's original message quoted below
    // for context. Both are escaped: the reply is typed by the developer,
    // but the original is user-supplied.
    private String buildFeedbackReplyHtml(String reply, String originalMessage, boolean spanish) {
        String heading = spanish ? "Respuesta a tu feedback" : "A reply to your feedback";
        String originalLabel = spanish ? "Tu mensaje" : "Your message";
        String footer = spanish
                ? "Este correo se envía desde una dirección que no admite respuestas. Si quieres añadir algo, envíanos más feedback desde Ajustes → Soporte en la app."
                : "This email comes from an address that doesn't accept replies. To add anything, send us more feedback from Settings → Support in the app.";
        String logoUrl = frontendUrl + "/icons/icon-192.png";

        return """
                <div style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; max-width: 480px; margin: 0 auto; padding: 40px 24px; color: #27272a;">
                    <table role="presentation" align="center" cellpadding="0" cellspacing="0" border="0" style="margin: 0 auto 36px;">
                        <tr>
                            <td style="vertical-align: middle; padding-right: 12px;">
                                <img src="%s" width="40" height="40" alt="RedCheck" style="display: block; border-radius: 11px;">
                            </td>
                            <td style="vertical-align: middle;">
                                <span style="font-size: 22px; font-weight: 900; letter-spacing: -0.02em; color: #18181b;">REDCHECK</span>
                            </td>
                        </tr>
                    </table>
                    <h1 style="font-size: 20px; font-weight: 700; margin: 0 0 16px;">%s</h1>
                    <p style="font-size: 14px; line-height: 1.6; color: #3f3f46; white-space: pre-wrap; margin: 0 0 28px;">%s</p>
                    <p style="font-size: 12px; font-weight: 600; color: #71717a; margin: 0 0 6px;">%s</p>
                    <div style="font-size: 13px; line-height: 1.6; color: #52525b; white-space: pre-wrap; border-left: 3px solid #e4e4e7; padding: 4px 0 4px 12px; margin: 0 0 32px;">%s</div>
                    <p style="font-size: 12px; line-height: 1.6; color: #a1a1aa; margin: 0;">%s</p>
                </div>
                """.formatted(logoUrl, heading, HtmlUtils.htmlEscape(reply), originalLabel,
                        HtmlUtils.htmlEscape(originalMessage), footer);
    }

    // Same visual language as the reset email below, with the message shown
    // verbatim (pre-wrap keeps the user's line breaks).
    private String buildFeedbackHtml(String category, String message, String authorEmail) {
        String author = authorEmail != null ? HtmlUtils.htmlEscape(authorEmail) : "—";
        String dashboardUrl = frontendUrl + "/developer";

        return """
                <div style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; max-width: 480px; margin: 0 auto; padding: 40px 24px; color: #27272a;">
                    <p style="font-size: 12px; font-weight: 700; letter-spacing: 0.08em; text-transform: uppercase; color: #dc2626; margin: 0 0 8px;">%s</p>
                    <h1 style="font-size: 20px; font-weight: 700; margin: 0 0 4px;">Nuevo feedback en RedCheck</h1>
                    <p style="font-size: 13px; color: #71717a; margin: 0 0 24px;">De %s</p>
                    <div style="font-size: 14px; line-height: 1.6; color: #3f3f46; white-space: pre-wrap; background-color: #f4f4f5; border-radius: 12px; padding: 16px; margin: 0 0 28px;">%s</div>
                    <a href="%s" style="display: inline-block; background-color: #dc2626; color: #ffffff; text-decoration: none; font-size: 14px; font-weight: 600; padding: 12px 24px; border-radius: 12px;">Abrir panel de desarrollador</a>
                </div>
                """.formatted(HtmlUtils.htmlEscape(category), author, HtmlUtils.htmlEscape(message), dashboardUrl);
    }

    // Deliberately minimal: one accent color (the brand red), generous
    // whitespace, a single CTA button, and the app's own brand lockup (icon
    // + wordmark) at the top — matches the frontend's clean/minimal visual
    // language (see redcheck-frontend's CLAUDE.md "Styling" note) rather
    // than a heavier marketing-style email.
    private String buildHtml(String resetLink, boolean spanish) {
        String heading = spanish ? "Restablece tu contraseña" : "Reset your password";
        String body = spanish
                ? "Hemos recibido una solicitud para restablecer la contraseña de tu cuenta de RedCheck. Este enlace caduca en 30 minutos."
                : "We received a request to reset your RedCheck account's password. This link expires in 30 minutes.";
        String button = spanish ? "Restablecer contraseña" : "Reset password";
        String ignore = spanish
                ? "Si no has solicitado esto, puedes ignorar este correo con tranquilidad."
                : "If you didn't request this, you can safely ignore this email.";

        String logoUrl = frontendUrl + "/icons/icon-192.png";

        // Full brand lockup (icon + "REDCHECK" wordmark), same pairing as
        // LoginPage.tsx's header — a table, not flexbox, since that's what
        // reliably centers/aligns across email clients (Outlook in
        // particular has no usable flexbox support).
        return """
                <div style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; max-width: 480px; margin: 0 auto; padding: 40px 24px; color: #27272a;">
                    <table role="presentation" align="center" cellpadding="0" cellspacing="0" border="0" style="margin: 0 auto 36px;">
                        <tr>
                            <td style="vertical-align: middle; padding-right: 12px;">
                                <img src="%s" width="40" height="40" alt="RedCheck" style="display: block; border-radius: 11px;">
                            </td>
                            <td style="vertical-align: middle;">
                                <span style="font-size: 22px; font-weight: 900; letter-spacing: -0.02em; color: #18181b;">REDCHECK</span>
                            </td>
                        </tr>
                    </table>
                    <h1 style="font-size: 20px; font-weight: 700; margin: 0 0 16px;">%s</h1>
                    <p style="font-size: 14px; line-height: 1.6; color: #52525b; margin: 0 0 28px;">%s</p>
                    <a href="%s" style="display: inline-block; background-color: #dc2626; color: #ffffff; text-decoration: none; font-size: 14px; font-weight: 600; padding: 12px 24px; border-radius: 12px;">%s</a>
                    <p style="font-size: 12px; line-height: 1.6; color: #a1a1aa; margin: 32px 0 0;">%s</p>
                </div>
                """.formatted(logoUrl, heading, body, resetLink, button, ignore);
    }
}

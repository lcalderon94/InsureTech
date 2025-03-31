package com.insurtech.notification.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.insurtech.notification.exception.DeliveryFailedException;
import com.insurtech.notification.model.entity.Notification;
import com.insurtech.notification.model.enums.DeliveryStatus;
import com.insurtech.notification.service.interfaces.EmailService;
import com.insurtech.notification.service.interfaces.TemplateService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.context.Context;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;
    private final TemplateService templateService;
    private final ObjectMapper objectMapper;

    @Value("${notification.email.enabled:true}")
    private boolean emailEnabled;

    @Value("${notification.email.from}")
    private String fromEmail;

    @Value("${notification.email.sender-name:InsureTech}")
    private String senderName;

    @Override
    public DeliveryStatus sendEmail(Notification notification) {
        log.info("Enviando email a {}: {}", notification.getRecipient(), notification.getSubject());

        if (!emailEnabled) {
            log.warn("Servicio de email deshabilitado. Email no enviado a: {}", notification.getRecipient());
            return DeliveryStatus.FAILED;
        }

        try {
            // Preparar destinatarios CC
            String[] ccAddresses = null;
            if (notification.getCcRecipients() != null && !notification.getCcRecipients().isBlank()) {
                ccAddresses = notification.getCcRecipients().split(",");
            }

            // Enviar email con HTML
            return sendHtmlEmail(
                    notification.getRecipient(),
                    ccAddresses,
                    notification.getSubject(),
                    notification.getContent()
            );
        } catch (Exception e) {
            log.error("Error enviando email: {}", e.getMessage(), e);
            throw new DeliveryFailedException("Error enviando email: " + e.getMessage(), e);
        }
    }

    @Override
    public DeliveryStatus sendTemplatedEmail(String to, String[] cc, String subject,
                                             String templateName, Map<String, Object> templateVariables) {
        log.info("Enviando email con plantilla {} a: {}", templateName, to);

        if (!emailEnabled) {
            log.warn("Servicio de email deshabilitado. Email no enviado a: {}", to);
            return DeliveryStatus.FAILED;
        }

        try {
            // Procesar plantilla Thymeleaf
            Context context = new Context();
            templateVariables.forEach(context::setVariable);

            String processedHtml = templateEngine.process(templateName, context);

            // Enviar email con HTML procesado
            return sendHtmlEmail(to, cc, subject, processedHtml);
        } catch (Exception e) {
            log.error("Error enviando email con plantilla: {}", e.getMessage(), e);
            throw new DeliveryFailedException("Error enviando email con plantilla: " + e.getMessage(), e);
        }
    }

    @Override
    public DeliveryStatus sendHtmlEmail(String to, String[] cc, String subject, String content) {
        if (!emailEnabled) {
            log.warn("Servicio de email deshabilitado. Email no enviado a: {}", to);
            return DeliveryStatus.FAILED;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message,
                    true, StandardCharsets.UTF_8.name());

            helper.setFrom(fromEmail, senderName);
            helper.setTo(to);

            if (cc != null && cc.length > 0) {
                helper.setCc(cc);
            }

            helper.setSubject(subject);
            helper.setText(content, true); // true indica HTML

            // Enviar mensaje
            mailSender.send(message);
            log.info("Email enviado exitosamente a: {}", to);

            return DeliveryStatus.SENT;
        } catch (Exception e) {
            log.error("Error enviando email: {}", e.getMessage(), e);
            throw new DeliveryFailedException("Error enviando email: " + e.getMessage(), e);
        }
    }

    @Override
    @Async("emailTaskExecutor")
    public CompletableFuture<DeliveryStatus> sendEmailAsync(Notification notification) {
        DeliveryStatus status = sendEmail(notification);
        return CompletableFuture.completedFuture(status);
    }

    /**
     * Extrae las variables de contexto de una notificación
     */
    private Map<String, Object> extractTemplateVariables(Notification notification) {
        if (notification.getDataContext() == null || notification.getDataContext().isBlank()) {
            return new HashMap<>();
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> variables = objectMapper.readValue(
                    notification.getDataContext(), Map.class);
            return variables;
        } catch (JsonProcessingException e) {
            log.warn("Error deserializando variables de contexto: {}", e.getMessage());
            return new HashMap<>();
        }
    }
}
package com.insurtech.notification.event.consumer;

import com.insurtech.notification.exception.NotificationException;
import com.insurtech.notification.model.dto.NotificationRequestDto;
import com.insurtech.notification.model.enums.NotificationPriority;
import com.insurtech.notification.model.enums.NotificationType;
import com.insurtech.notification.service.interfaces.NotificationService;
import com.insurtech.notification.service.interfaces.TemplateService;
import com.insurtech.notification.util.SmsTemplates;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.support.Acknowledgment;

import java.util.Map;

@Slf4j
public abstract class BaseEventConsumer {

    @Autowired
    protected NotificationService notificationService;

    @Autowired
    protected TemplateService templateService;

    /**
     * Procesa un evento recibido desde Kafka
     */
    protected void processEvent(Map<String, Object> event, Acknowledgment acknowledgment) {
        try {
            String eventType = determineEventType(event);
            String eventId = event.get("eventId") != null ? event.get("eventId").toString() : "unknown";

            log.info("Procesando evento: {} con ID: {}", eventType, eventId);

            // Extraer variables específicas del evento
            Map<String, Object> variables = extractVariables(event);

            // Extraer datos de contacto
            String email = extractEmail(event);
            String phone = extractPhone(event);

            // Enviar notificaciones por todos los canales disponibles
            processNotificationsForAllChannels(eventType, email, phone, variables, eventId);

            acknowledgment.acknowledge();
            log.info("Evento procesado correctamente: {}", eventId);
        } catch (Exception e) {
            String eventId = event.get("eventId") != null ? event.get("eventId").toString() : "unknown";
            log.error("Error procesando evento {}: {}", eventId, e.getMessage(), e);
            // Decisión según política de errores (acknowledge o no)
            acknowledgment.acknowledge(); // Por defecto, reconocemos para evitar mensajes "poison pill"
        }
    }

    /**
     * Envía notificaciones por todos los canales disponibles
     */
    protected void processNotificationsForAllChannels(
            String eventType,
            String email,
            String phone,
            Map<String, Object> variables,
            String sourceEventId) {

        // Si hay email, enviar por email
        if (email != null && !email.trim().isEmpty()) {
            try {
                processEmailNotification(eventType, email, variables, sourceEventId);
            } catch (Exception e) {
                log.error("Error enviando email para evento {}: {}", eventType, e.getMessage());
            }
        } else {
            log.warn("No hay dirección de email disponible para notificación: {}", eventType);
        }

        // Si hay teléfono, enviar también por SMS
        if (phone != null && !phone.trim().isEmpty()) {
            try {
                processSmsNotification(eventType, phone, variables, sourceEventId);
            } catch (Exception e) {
                log.error("Error enviando SMS para evento {}: {}", eventType, e.getMessage());
            }
        } else {
            log.warn("No hay número de teléfono disponible para notificación: {}", eventType);
        }
    }

    /**
     * Procesa notificación por SMS
     */
    protected void processSmsNotification(String eventType, String phoneNumber, Map<String, Object> variables, String sourceEventId) {
        try {
            // Obtener y procesar la plantilla SMS
            String smsContent = SmsTemplates.processEventTemplate(eventType, variables);

            // Crear solicitud de notificación SMS
            NotificationRequestDto smsRequest = NotificationRequestDto.builder()
                    .type(NotificationType.SMS)
                    .recipient(phoneNumber)
                    .content(smsContent)
                    .priority(determinePriority(eventType))
                    .sourceEventId(sourceEventId)
                    .sourceEventType(eventType)
                    .build();

            // Enviar la notificación
            notificationService.createNotification(smsRequest);
            log.info("SMS enviado para evento {} a {}", eventType, phoneNumber);
        } catch (Exception e) {
            log.error("Error enviando SMS para evento {}: {}", eventType, e.getMessage());
            throw new NotificationException("Error enviando SMS: " + e.getMessage(), e);
        }
    }

    /**
     * Procesa notificación por Email
     */
    protected void processEmailNotification(String eventType, String email, Map<String, Object> variables, String sourceEventId) {
        try {
            // Mapear tipo de evento a código de plantilla de email
            String templateCode = mapEventTypeToEmailTemplate(eventType);

            // Crear solicitud de notificación Email
            NotificationRequestDto emailRequest = NotificationRequestDto.builder()
                    .type(NotificationType.EMAIL)
                    .recipient(email)
                    .templateCode(templateCode)
                    .templateVariables(variables)
                    .priority(determinePriority(eventType))
                    .sourceEventId(sourceEventId)
                    .sourceEventType(eventType)
                    .build();

            // Enviar la notificación
            notificationService.createNotification(emailRequest);
            log.info("Email enviado para evento {} a {}", eventType, email);
        } catch (Exception e) {
            log.error("Error enviando Email para evento {}: {}", eventType, e.getMessage());
            throw new NotificationException("Error enviando Email: " + e.getMessage(), e);
        }
    }

    /**
     * Determina la prioridad según el tipo de evento
     */
    protected NotificationPriority determinePriority(String eventType) {
        // Eventos de alta prioridad
        if (eventType.contains("failed") ||
                eventType.contains("cancelled") ||
                eventType.contains("approved") ||
                eventType.contains("rejected") ||
                eventType.contains("alert") ||
                eventType.contains("expiring") ||
                eventType.contains("password.reset")) {
            return NotificationPriority.HIGH;
        }

        // Eventos de prioridad media
        if (eventType.contains("created") ||
                eventType.contains("renewed") ||
                eventType.contains("processed")) {
            return NotificationPriority.MEDIUM;
        }

        // Por defecto
        return NotificationPriority.LOW;
    }

    /**
     * Convierte tipo de evento a código de plantilla de email
     */
    protected String mapEventTypeToEmailTemplate(String eventType) {
        // Convertir formato "service.event" a "service-event" para plantillas
        return eventType.replace(".", "-");
    }

    // Métodos abstractos que cada consumidor debe implementar

    /**
     * Determina el tipo de evento
     */
    protected abstract String determineEventType(Map<String, Object> event);

    /**
     * Extrae variables específicas del tipo de evento
     */
    protected abstract Map<String, Object> extractVariables(Map<String, Object> event);

    /**
     * Extrae email del evento
     */
    protected abstract String extractEmail(Map<String, Object> event);

    /**
     * Extrae teléfono del evento
     */
    protected abstract String extractPhone(Map<String, Object> event);
}
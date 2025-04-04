package com.insurtech.notification.event.consumer;

import com.insurtech.notification.event.model.CustomerEvent;
import com.insurtech.notification.exception.NotificationException;
import com.insurtech.notification.exception.TemplateNotFoundException;
import com.insurtech.notification.model.dto.NotificationRequestDto;
import com.insurtech.notification.model.entity.NotificationTemplate;
import com.insurtech.notification.model.enums.NotificationPriority;
import com.insurtech.notification.model.enums.NotificationType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
@Slf4j
public class CustomerEventConsumer extends BaseEventConsumer<CustomerEvent> {

    // Escuchar en los tópicos exactos que produce CustomerEventProducer
    @KafkaListener(topics = "customer.created", containerFactory = "customerKafkaListenerContainerFactory")
    public void consumeCustomerCreated(CustomerEvent event, Acknowledgment acknowledgment) {
        log.info("Recibido evento de cliente creado: {}", event.getDocumentNumber());
        processEvent(event, acknowledgment);
    }

    @KafkaListener(topics = "customer.updated", containerFactory = "customerKafkaListenerContainerFactory")
    public void consumeCustomerUpdated(CustomerEvent event, Acknowledgment acknowledgment) {
        log.info("Recibido evento de cliente actualizado: {}", event.getDocumentNumber());
        processEvent(event, acknowledgment);
    }

    @KafkaListener(topics = "customer.status.changed", containerFactory = "customerKafkaListenerContainerFactory")
    public void consumeCustomerStatusChanged(CustomerEvent event, Acknowledgment acknowledgment) {
        log.info("Recibido evento de cambio de estado de cliente: {}", event.getDocumentNumber());
        processEvent(event, acknowledgment);
    }

    @Override
    protected void processEventInternal(CustomerEvent event) throws NotificationException {
        log.info("Procesando evento de cliente: {} - {}",
                event.getDocumentNumber(), event.getActionType());

        // Determinar la plantilla según el tipo de acción
        String templateCode = determineTemplateCode(event);
        if (templateCode == null) {
            log.info("No se requiere notificación para este evento de cliente: {}", event.getActionType());
            return;
        }

        // Preparar datos para la plantilla
        Map<String, Object> templateData = prepareTemplateData(event);

        // Buscar la plantilla
        Optional<NotificationTemplate> template = templateService.findTemplateByCode(templateCode);
        if (template.isEmpty()) {
            throw new TemplateNotFoundException("No se encontró la plantilla: " + templateCode);
        }

        // Crear y enviar notificación por email
        if (event.getEmail() != null && !event.getEmail().isBlank()) {
            NotificationRequestDto emailRequest = NotificationRequestDto.builder()
                    .type(NotificationType.EMAIL)
                    .priority(determinePriority(event))
                    .recipient(event.getEmail())
                    .templateCode(templateCode)
                    .templateVariables(templateData)
                    .sourceEventId(event.getEventId().toString())
                    .sourceEventType(event.getEventType())
                    .build();

            notificationService.createNotification(emailRequest);
        }

        // Enviar SMS para eventos de verificación o importante
        if (shouldSendSms(event) && event.getPhone() != null && !event.getPhone().isBlank()) {
            NotificationRequestDto smsRequest = NotificationRequestDto.builder()
                    .type(NotificationType.SMS)
                    .priority(NotificationPriority.HIGH)
                    .recipient(event.getPhone())
                    .templateCode(templateCode + "_SMS")
                    .templateVariables(templateData)
                    .sourceEventId(event.getEventId().toString())
                    .sourceEventType(event.getEventType())
                    .build();

            notificationService.createNotification(smsRequest);
        }
    }

    private String determineTemplateCode(CustomerEvent event) {
        switch (event.getActionType()) {
            case "CREATED":
                return "customer_welcome";
            case "UPDATED":
                return "customer_profile_updated";
            case "VERIFIED":
                return "customer_verified";
            case "DEACTIVATED":
                return "customer_account_deactivated";
            case "PASSWORD_RESET":
                return "customer_password_reset";
            case "LOGIN_ATTEMPT":
                return "customer_suspicious_login";
            default:
                return null;
        }
    }

    private Map<String, Object> prepareTemplateData(CustomerEvent event) {
        Map<String, Object> data = new HashMap<>();
        data.put("customerName", event.getFirstName() + " " + event.getLastName());
        data.put("firstName", event.getFirstName());
        data.put("lastName", event.getLastName());
        data.put("documentNumber", event.getDocumentNumber());
        data.put("email", event.getEmail());
        data.put("phone", event.getPhone());
        data.put("status", event.getCustomerStatus());
        data.put("actionType", event.getActionType());
        data.put("address", event.getAddress());
        data.put("city", event.getCity());
        data.put("zipCode", event.getZipCode());
        data.put("country", event.getCountry());

        // Agregar detalles adicionales si existen
        if (event.getAdditionalDetails() != null) {
            data.putAll(event.getAdditionalDetails());
        }

        return data;
    }

    private NotificationPriority determinePriority(CustomerEvent event) {
        switch (event.getActionType()) {
            case "VERIFIED":
            case "DEACTIVATED":
            case "PASSWORD_RESET":
            case "LOGIN_ATTEMPT":
                return NotificationPriority.HIGH;
            case "CREATED":
                return NotificationPriority.MEDIUM;
            default:
                return NotificationPriority.LOW;
        }
    }

    private boolean shouldSendSms(CustomerEvent event) {
        return "VERIFIED".equals(event.getActionType()) ||
                "PASSWORD_RESET".equals(event.getActionType()) ||
                "LOGIN_ATTEMPT".equals(event.getActionType());
    }
}
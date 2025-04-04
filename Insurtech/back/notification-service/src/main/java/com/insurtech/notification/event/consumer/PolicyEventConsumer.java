package com.insurtech.notification.event.consumer;

import com.insurtech.notification.event.model.PolicyEvent;
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

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@Slf4j
public class PolicyEventConsumer extends BaseEventConsumer<PolicyEvent> {

    // Escuchar en los tópicos exactos que produce PolicyEventProducer
    @KafkaListener(topics = "policy.created", containerFactory = "policyKafkaListenerContainerFactory")
    public void consumePolicyCreated(PolicyEvent event, Acknowledgment acknowledgment) {
        log.info("Recibido evento de póliza creada: {}", event.getPolicyNumber());
        processEvent(event, acknowledgment);
    }

    @KafkaListener(topics = "policy.updated", containerFactory = "policyKafkaListenerContainerFactory")
    public void consumePolicyUpdated(PolicyEvent event, Acknowledgment acknowledgment) {
        log.info("Recibido evento de póliza actualizada: {}", event.getPolicyNumber());
        processEvent(event, acknowledgment);
    }

    @KafkaListener(topics = "policy.status.changed", containerFactory = "policyKafkaListenerContainerFactory")
    public void consumePolicyStatusChanged(PolicyEvent event, Acknowledgment acknowledgment) {
        log.info("Recibido evento de cambio de estado de póliza: {}", event.getPolicyNumber());
        processEvent(event, acknowledgment);
    }

    @Override
    protected void processEventInternal(PolicyEvent event) throws NotificationException {
        log.info("Procesando evento de póliza: {}, tipo: {}", event.getPolicyNumber(), event.getActionType());

        // Determinar qué plantilla usar según el tipo de evento
        String templateCode = determineTemplateCode(event);
        if (templateCode == null) {
            log.info("No se requiere notificación para este evento de póliza: {}", event.getActionType());
            return;
        }

        // Preparar los datos para la plantilla
        Map<String, Object> templateData = prepareTemplateData(event);

        // Buscar la plantilla
        Optional<NotificationTemplate> template = templateService.findTemplateByCode(templateCode);
        if (template.isEmpty()) {
            throw new TemplateNotFoundException("No se encontró la plantilla: " + templateCode);
        }

        // Crear y enviar notificación por email
        if (event.getCustomerEmail() != null && !event.getCustomerEmail().isBlank()) {
            NotificationRequestDto emailRequest = NotificationRequestDto.builder()
                    .type(NotificationType.EMAIL)
                    .priority(determinePriority(event))
                    .recipient(event.getCustomerEmail())
                    .templateCode(templateCode)
                    .templateVariables(templateData)
                    .sourceEventId(event.getEventId().toString())
                    .sourceEventType(event.getEventType())
                    .build();

            notificationService.createNotification(emailRequest);
        }

        // Crear y enviar notificación por SMS para eventos críticos
        if (isCriticalEvent(event) && event.getCustomerPhone() != null && !event.getCustomerPhone().isBlank()) {
            NotificationRequestDto smsRequest = NotificationRequestDto.builder()
                    .type(NotificationType.SMS)
                    .priority(NotificationPriority.HIGH)
                    .recipient(event.getCustomerPhone())
                    .templateCode(templateCode + "_SMS")
                    .templateVariables(templateData)
                    .sourceEventId(event.getEventId().toString())
                    .sourceEventType(event.getEventType())
                    .build();

            notificationService.createNotification(smsRequest);
        }
    }

    private String determineTemplateCode(PolicyEvent event) {
        switch (event.getActionType()) {
            case "CREATED":
                return "policy_created";
            case "RENEWED":
                return "policy_renewed";
            case "CANCELLED":
                return "policy_cancelled";
            case "UPDATED":
                return "policy_updated";
            case "EXPIRING_SOON":
                // Solo notificar si la fecha de vencimiento está a menos de 30 días
                if (event.getExpirationDate() != null &&
                        ChronoUnit.DAYS.between(LocalDate.now(), event.getExpirationDate()) <= 30) {
                    return "policy_expiring_soon";
                }
                return null;
            default:
                return null;
        }
    }

    private Map<String, Object> prepareTemplateData(PolicyEvent event) {
        Map<String, Object> data = new HashMap<>();
        data.put("policyNumber", event.getPolicyNumber());
        data.put("policyType", event.getPolicyType());
        data.put("customerName", event.getCustomerName());
        data.put("effectiveDate", event.getEffectiveDate());
        data.put("expirationDate", event.getExpirationDate());
        data.put("status", event.getPolicyStatus());
        data.put("actionType", event.getActionType());

        // Agregar detalles adicionales si existen
        if (event.getAdditionalDetails() != null) {
            data.putAll(event.getAdditionalDetails());
        }

        return data;
    }

    private NotificationPriority determinePriority(PolicyEvent event) {
        switch (event.getActionType()) {
            case "CANCELLED":
            case "EXPIRING_SOON":
                return NotificationPriority.HIGH;
            case "CREATED":
            case "RENEWED":
                return NotificationPriority.MEDIUM;
            default:
                return NotificationPriority.LOW;
        }
    }

    private boolean isCriticalEvent(PolicyEvent event) {
        List<String> criticalEvents = List.of("CANCELLED", "EXPIRING_SOON");
        return criticalEvents.contains(event.getActionType());
    }
}
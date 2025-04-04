package com.insurtech.notification.event.consumer;

import com.insurtech.notification.event.model.ClaimEvent;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@Slf4j
public class ClaimEventConsumer extends BaseEventConsumer<ClaimEvent> {

    // Escuchar en los tópicos exactos que produce ClaimEventProducer
    @KafkaListener(topics = "claim.created", containerFactory = "claimKafkaListenerContainerFactory")
    public void consumeClaimCreated(ClaimEvent event, Acknowledgment acknowledgment) {
        log.info("Recibido evento de reclamación creada: {}", event.getClaimNumber());
        processEvent(event, acknowledgment);
    }

    @KafkaListener(topics = "claim.updated", containerFactory = "claimKafkaListenerContainerFactory")
    public void consumeClaimUpdated(ClaimEvent event, Acknowledgment acknowledgment) {
        log.info("Recibido evento de reclamación actualizada: {}", event.getClaimNumber());
        processEvent(event, acknowledgment);
    }

    @KafkaListener(topics = "claim.status.changed", containerFactory = "claimKafkaListenerContainerFactory")
    public void consumeClaimStatusChanged(ClaimEvent event, Acknowledgment acknowledgment) {
        log.info("Recibido evento de cambio de estado de reclamación: {}", event.getClaimNumber());
        processEvent(event, acknowledgment);
    }

    @KafkaListener(topics = "claim.item.added", containerFactory = "claimKafkaListenerContainerFactory")
    public void consumeClaimItemAdded(ClaimEvent event, Acknowledgment acknowledgment) {
        log.info("Recibido evento de ítem añadido a reclamación: {}", event.getClaimNumber());
        processEvent(event, acknowledgment);
    }

    @Override
    protected void processEventInternal(ClaimEvent event) throws NotificationException {
        log.info("Procesando evento de reclamación: {} - Cambio de estado: {} -> {}",
                event.getClaimNumber(), event.getPreviousStatus(), event.getClaimStatus());

        // Solo notificar cambios de estado o si es un evento explícito
        if (event.getPreviousStatus() != null && event.getPreviousStatus().equals(event.getClaimStatus())
                && !"NOTIFICATION_REQUESTED".equals(event.getEventType())) {
            log.info("Sin cambios significativos en la reclamación, no se enviará notificación");
            return;
        }

        // Determinar la plantilla según el estado de la reclamación
        String templateCode = determineTemplateCode(event);
        if (templateCode == null) {
            log.info("No se requiere notificación para este estado de reclamación: {}", event.getClaimStatus());
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

        // Enviar SMS para cambios de estado importantes
        if (isImportantStatusChange(event) && event.getCustomerPhone() != null && !event.getCustomerPhone().isBlank()) {
            NotificationRequestDto smsRequest = NotificationRequestDto.builder()
                    .type(NotificationType.SMS)
                    .priority(determinePriority(event))
                    .recipient(event.getCustomerPhone())
                    .templateCode(templateCode + "_SMS")
                    .templateVariables(templateData)
                    .sourceEventId(event.getEventId().toString())
                    .sourceEventType(event.getEventType())
                    .build();

            notificationService.createNotification(smsRequest);
        }
    }

    private String determineTemplateCode(ClaimEvent event) {
        switch (event.getClaimStatus()) {
            case "SUBMITTED":
                return "claim_submitted";
            case "IN_REVIEW":
                return "claim_in_review";
            case "APPROVED":
                return "claim_approved";
            case "REJECTED":
                return "claim_rejected";
            case "PENDING_INFO":
                return "claim_pending_info";
            case "PAYMENT_PROCESSED":
                return "claim_payment_processed";
            default:
                return "claim_status_update";
        }
    }

    private Map<String, Object> prepareTemplateData(ClaimEvent event) {
        Map<String, Object> data = new HashMap<>();
        data.put("claimNumber", event.getClaimNumber());
        data.put("policyNumber", event.getPolicyNumber());
        data.put("customerName", event.getCustomerName());
        data.put("claimStatus", formatStatusForDisplay(event.getClaimStatus()));
        data.put("previousStatus", formatStatusForDisplay(event.getPreviousStatus()));
        data.put("claimType", event.getClaimType());
        data.put("incidentDate", event.getIncidentDate());
        data.put("reportDate", event.getReportDate());
        data.put("claimAmount", event.getClaimAmount());
        data.put("approvedAmount", event.getApprovedAmount());
        data.put("description", event.getDescription());
        data.put("statusNotes", event.getStatusNotes());

        // Agregar detalles adicionales si existen
        if (event.getAdditionalDetails() != null) {
            data.putAll(event.getAdditionalDetails());
        }

        return data;
    }

    private NotificationPriority determinePriority(ClaimEvent event) {
        switch (event.getClaimStatus()) {
            case "APPROVED":
            case "REJECTED":
            case "PAYMENT_PROCESSED":
                return NotificationPriority.HIGH;
            case "IN_REVIEW":
            case "PENDING_INFO":
                return NotificationPriority.MEDIUM;
            default:
                return NotificationPriority.LOW;
        }
    }

    private boolean isImportantStatusChange(ClaimEvent event) {
        List<String> importantStatuses = List.of(
                "APPROVED", "REJECTED", "PAYMENT_PROCESSED", "PENDING_INFO");
        return importantStatuses.contains(event.getClaimStatus());
    }

    private String formatStatusForDisplay(String status) {
        if (status == null) {
            return null;
        }

        // Convertir formato SNAKE_CASE a Título
        return status.replace("_", " ")
                .toLowerCase()
                .replace(
                        status.toLowerCase().charAt(0),
                        Character.toUpperCase(status.charAt(0))
                );
    }
}
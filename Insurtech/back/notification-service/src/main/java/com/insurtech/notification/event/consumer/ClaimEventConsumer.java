package com.insurtech.notification.event.consumer;

import com.insurtech.notification.event.model.ClaimEvent;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ClaimEventConsumer extends BaseEventConsumer<ClaimEvent> {

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

    @KafkaListener(topics = "claim.approved", containerFactory = "claimKafkaListenerContainerFactory")
    public void consumeClaimApproved(ClaimEvent event, Acknowledgment acknowledgment) {
        log.info("Recibido evento de reclamación aprobada: {}", event.getClaimNumber());
        processEvent(event, acknowledgment);
    }

    @KafkaListener(topics = "claim.rejected", containerFactory = "claimKafkaListenerContainerFactory")
    public void consumeClaimRejected(ClaimEvent event, Acknowledgment acknowledgment) {
        log.info("Recibido evento de reclamación rechazada: {}", event.getClaimNumber());
        processEvent(event, acknowledgment);
    }

    @KafkaListener(topics = "claim.document.required", containerFactory = "claimKafkaListenerContainerFactory")
    public void consumeClaimDocumentRequired(ClaimEvent event, Acknowledgment acknowledgment) {
        log.info("Recibido evento de documentación requerida para reclamación: {}", event.getClaimNumber());
        processEvent(event, acknowledgment);
    }

    @Override
    protected String determineEventType(ClaimEvent event) {
        if (event.getClaimStatus() != null) {
            if ("APPROVED".equals(event.getClaimStatus())) {
                return "claim.approved";
            } else if ("REJECTED".equals(event.getClaimStatus())) {
                return "claim.rejected";
            } else if (event.getPreviousStatus() != null && !event.getPreviousStatus().equals(event.getClaimStatus())) {
                return "claim.status.changed";
            }
        }

        if (event.getEventType() != null) {
            return event.getEventType();
        }

        // Determinar basado en la presencia de campos específicos
        if (event.getAdditionalDetails() != null && event.getAdditionalDetails().containsKey("documentsRequired")) {
            return "claim.document.required";
        }

        return "claim.updated"; // Valor por defecto
    }

    @Override
    protected Map<String, Object> extractVariables(ClaimEvent event) {
        Map<String, Object> variables = new HashMap<>();

        // Datos básicos
        variables.put("claimNumber", event.getClaimNumber());
        variables.put("claimId", event.getClaimId().toString());
        variables.put("policyNumber", event.getPolicyNumber());
        variables.put("customerName", event.getCustomerName());
        variables.put("claimType", event.getClaimType());

        // Referencia
        variables.put("claimReference", event.getClaimNumber()); // O una referencia específica si existe

        // Estado
        if (event.getClaimStatus() != null) {
            variables.put("newStatus", formatStatus(event.getClaimStatus()));
            if (event.getPreviousStatus() != null) {
                variables.put("previousStatus", formatStatus(event.getPreviousStatus()));
            }
        }

        // Formatear fechas
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        if (event.getIncidentDate() != null) {
            variables.put("incidentDate", event.getIncidentDate().format(dateFormatter));
        }
        if (event.getReportDate() != null) {
            variables.put("reportDate", event.getReportDate().format(dateFormatter));
        }

        // Importes
        if (event.getClaimAmount() != null) {
            variables.put("claimAmount", formatCurrency(event.getClaimAmount()));
        }
        if (event.getApprovedAmount() != null) {
            variables.put("approvedAmount", formatCurrency(event.getApprovedAmount()));
        }

        // Datos específicos para aprobación
        if ("APPROVED".equals(event.getClaimStatus())) {
            variables.put("paymentDays", "5"); // O calcularlo dinámicamente
            variables.put("approvalDate", java.time.LocalDate.now().format(dateFormatter));
        }

        // Datos específicos para rechazo
        if ("REJECTED".equals(event.getClaimStatus())) {
            variables.put("rejectionReasonShort", getShortRejectionReason(event));
        }

        // Datos para documento requerido
        if (event.getAdditionalDetails() != null && event.getAdditionalDetails().containsKey("documentsRequired")) {
            // Calcular una fecha límite (ejemplo: 15 días)
            variables.put("deadlineDate", java.time.LocalDate.now().plusDays(15).format(dateFormatter));
        }

        // Descripción
        if (event.getDescription() != null) {
            variables.put("description", event.getDescription());
        }

        // Notas
        if (event.getStatusNotes() != null) {
            variables.put("statusNotes", event.getStatusNotes());
        }

        // Agregar detalles adicionales
        if (event.getAdditionalDetails() != null) {
            variables.putAll(event.getAdditionalDetails());
        }

        return variables;
    }

    @Override
    protected String extractEmail(ClaimEvent event) {
        if (event.getCustomerEmail() != null && !event.getCustomerEmail().trim().isEmpty()) {
            return event.getCustomerEmail();
        }

        if (event.getAdditionalDetails() != null) {
            Object email = event.getAdditionalDetails().get("customerEmail");
            if (email != null && !email.toString().trim().isEmpty()) {
                return email.toString();
            }
        }

        return null;
    }

    @Override
    protected String extractPhone(ClaimEvent event) {
        if (event.getCustomerPhone() != null && !event.getCustomerPhone().trim().isEmpty()) {
            return event.getCustomerPhone();
        }

        if (event.getAdditionalDetails() != null) {
            Object phone = event.getAdditionalDetails().get("customerPhone");
            if (phone != null && !phone.toString().trim().isEmpty()) {
                return phone.toString();
            }
        }

        return null;
    }

    // Métodos auxiliares

    private String formatStatus(String status) {
        if (status == null) {
            return "";
        }

        // Convertir de SNAKE_CASE a formato normal
        return status.replace("_", " ")
                .toLowerCase()
                .replace(
                        status.toLowerCase().charAt(0),
                        Character.toUpperCase(status.charAt(0))
                );
    }

    private String formatCurrency(java.math.BigDecimal amount) {
        if (amount == null) {
            return "0.00";
        }
        return amount.setScale(2, java.math.RoundingMode.HALF_UP).toString();
    }

    private String getShortRejectionReason(ClaimEvent event) {
        if (event.getStatusNotes() != null && !event.getStatusNotes().isEmpty()) {
            // Devolver versión resumida de las notas (primeros 50 caracteres o primera frase)
            String notes = event.getStatusNotes();
            if (notes.length() > 50) {
                return notes.substring(0, 47) + "...";
            }
            return notes;
        }

        if (event.getAdditionalDetails() != null && event.getAdditionalDetails().containsKey("rejectionReason")) {
            return event.getAdditionalDetails().get("rejectionReason").toString();
        }

        return "Fuera de cobertura"; // Razón genérica
    }
}
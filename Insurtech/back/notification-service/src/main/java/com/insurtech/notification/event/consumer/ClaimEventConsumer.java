package com.insurtech.notification.event.consumer;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ClaimEventConsumer extends BaseEventConsumer {

    @KafkaListener(topics = "claim.created", containerFactory = "kafkaListenerContainerFactory")
    public void consumeClaimCreated(Map<String, Object> event, Acknowledgment acknowledgment) {
        log.info("Recibido evento de reclamación creada: {}", event.get("claimNumber"));
        processEvent(event, acknowledgment);
    }

    @KafkaListener(topics = "claim.updated", containerFactory = "kafkaListenerContainerFactory")
    public void consumeClaimUpdated(Map<String, Object> event, Acknowledgment acknowledgment) {
        log.info("Recibido evento de reclamación actualizada: {}", event.get("claimNumber"));
        processEvent(event, acknowledgment);
    }

    @KafkaListener(topics = "claim.status.changed", containerFactory = "kafkaListenerContainerFactory")
    public void consumeClaimStatusChanged(Map<String, Object> event, Acknowledgment acknowledgment) {
        log.info("Recibido evento de cambio de estado de reclamación: {}", event.get("claimNumber"));
        processEvent(event, acknowledgment);
    }

    @KafkaListener(topics = "claim.approved", containerFactory = "kafkaListenerContainerFactory")
    public void consumeClaimApproved(Map<String, Object> event, Acknowledgment acknowledgment) {
        log.info("Recibido evento de reclamación aprobada: {}", event.get("claimNumber"));
        processEvent(event, acknowledgment);
    }

    @KafkaListener(topics = "claim.rejected", containerFactory = "kafkaListenerContainerFactory")
    public void consumeClaimRejected(Map<String, Object> event, Acknowledgment acknowledgment) {
        log.info("Recibido evento de reclamación rechazada: {}", event.get("claimNumber"));
        processEvent(event, acknowledgment);
    }

    @KafkaListener(topics = "claim.document.required", containerFactory = "kafkaListenerContainerFactory")
    public void consumeClaimDocumentRequired(Map<String, Object> event, Acknowledgment acknowledgment) {
        log.info("Recibido evento de documentación requerida para reclamación: {}", event.get("claimNumber"));
        processEvent(event, acknowledgment);
    }

    @Override
    protected String determineEventType(Map<String, Object> event) {
        String claimStatus = getString(event, "claimStatus");
        if (claimStatus != null) {
            if ("APPROVED".equals(claimStatus)) {
                return "claim.approved";
            } else if ("REJECTED".equals(claimStatus)) {
                return "claim.rejected";
            } else if (event.get("previousStatus") != null &&
                    !event.get("previousStatus").equals(claimStatus)) {
                return "claim.status.changed";
            }
        }

        if (event.get("eventType") != null) {
            return event.get("eventType").toString();
        }

        // Determinar basado en la presencia de campos específicos
        if (event.get("additionalDetails") != null && event.get("additionalDetails") instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> additionalDetails = (Map<String, Object>) event.get("additionalDetails");
            if (additionalDetails.containsKey("documentsRequired")) {
                return "claim.document.required";
            }
        }

        return "claim.updated"; // Valor por defecto
    }

    @Override
    protected Map<String, Object> extractVariables(Map<String, Object> event) {
        Map<String, Object> variables = new HashMap<>();

        // Datos básicos
        variables.put("claimNumber", getString(event, "claimNumber"));

        if (event.get("claimId") != null) {
            variables.put("claimId", event.get("claimId").toString());
        }

        variables.put("policyNumber", getString(event, "policyNumber"));
        variables.put("customerName", getString(event, "customerName"));
        variables.put("claimType", getString(event, "claimType"));

        // Referencia
        variables.put("claimReference", getString(event, "claimNumber")); // O una referencia específica si existe

        // Estado
        String claimStatus = getString(event, "claimStatus");
        if (claimStatus != null) {
            variables.put("newStatus", formatStatus(claimStatus));

            String previousStatus = getString(event, "previousStatus");
            if (previousStatus != null) {
                variables.put("previousStatus", formatStatus(previousStatus));
            }
        }

        // Formatear fechas
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        Object incidentDateObj = event.get("incidentDate");
        if (incidentDateObj != null) {
            LocalDate incidentDate = parseDate(incidentDateObj);
            variables.put("incidentDate", incidentDate.format(dateFormatter));
        }

        Object reportDateObj = event.get("reportDate");
        if (reportDateObj != null) {
            LocalDate reportDate = parseDate(reportDateObj);
            variables.put("reportDate", reportDate.format(dateFormatter));
        }

        // Importes
        Object claimAmountObj = event.get("claimAmount");
        if (claimAmountObj != null) {
            variables.put("claimAmount", formatCurrency(claimAmountObj));
        }

        Object approvedAmountObj = event.get("approvedAmount");
        if (approvedAmountObj != null) {
            variables.put("approvedAmount", formatCurrency(approvedAmountObj));
        }

        // Datos específicos para aprobación
        if ("APPROVED".equals(claimStatus)) {
            variables.put("paymentDays", "5"); // O calcularlo dinámicamente
            variables.put("approvalDate", LocalDate.now().format(dateFormatter));
        }

        // Datos específicos para rechazo
        if ("REJECTED".equals(claimStatus)) {
            variables.put("rejectionReasonShort", getShortRejectionReason(event));
        }

        // Datos para documento requerido
        if (event.get("additionalDetails") != null && event.get("additionalDetails") instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> additionalDetails = (Map<String, Object>) event.get("additionalDetails");
            if (additionalDetails.containsKey("documentsRequired")) {
                // Calcular una fecha límite (ejemplo: 15 días)
                variables.put("deadlineDate", LocalDate.now().plusDays(15).format(dateFormatter));
            }
        }

        // Descripción
        variables.put("description", getString(event, "description"));

        // Notas
        variables.put("statusNotes", getString(event, "statusNotes"));

        // Agregar detalles adicionales
        if (event.get("additionalDetails") != null && event.get("additionalDetails") instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> additionalDetails = (Map<String, Object>) event.get("additionalDetails");
            variables.putAll(additionalDetails);
        }

        return variables;
    }

    @Override
    protected String extractEmail(Map<String, Object> event) {
        String email = getString(event, "customerEmail");
        if (email != null && !email.trim().isEmpty()) {
            return email;
        }

        if (event.get("additionalDetails") != null && event.get("additionalDetails") instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> additionalDetails = (Map<String, Object>) event.get("additionalDetails");
            Object emailObj = additionalDetails.get("customerEmail");
            if (emailObj != null && !emailObj.toString().trim().isEmpty()) {
                return emailObj.toString();
            }
        }

        return null;
    }

    @Override
    protected String extractPhone(Map<String, Object> event) {
        String phone = getString(event, "customerPhone");
        if (phone != null && !phone.trim().isEmpty()) {
            return phone;
        }

        if (event.get("additionalDetails") != null && event.get("additionalDetails") instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> additionalDetails = (Map<String, Object>) event.get("additionalDetails");
            Object phoneObj = additionalDetails.get("customerPhone");
            if (phoneObj != null && !phoneObj.toString().trim().isEmpty()) {
                return phoneObj.toString();
            }
        }

        return null;
    }

    // Métodos auxiliares

    private String getString(Map<String, Object> map, String key) {
        return map.get(key) != null ? map.get(key).toString() : null;
    }

    private LocalDate parseDate(Object dateObj) {
        if (dateObj instanceof LocalDate) {
            return (LocalDate) dateObj;
        } else if (dateObj instanceof String) {
            return LocalDate.parse((String) dateObj);
        } else if (dateObj instanceof Long) {
            return LocalDate.ofEpochDay((Long) dateObj / (24 * 60 * 60 * 1000));
        } else {
            return LocalDate.now(); // Valor por defecto
        }
    }

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

    private String formatCurrency(Object amount) {
        if (amount == null) {
            return "0.00";
        }

        BigDecimal numericAmount;
        if (amount instanceof BigDecimal) {
            numericAmount = (BigDecimal) amount;
        } else if (amount instanceof Number) {
            numericAmount = new BigDecimal(amount.toString());
        } else {
            try {
                numericAmount = new BigDecimal(amount.toString());
            } catch (Exception e) {
                log.warn("No se pudo convertir el monto a BigDecimal: {}", amount);
                return "0.00";
            }
        }

        return numericAmount.setScale(2, RoundingMode.HALF_UP).toString();
    }

    private String getShortRejectionReason(Map<String, Object> event) {
        String statusNotes = getString(event, "statusNotes");
        if (statusNotes != null && !statusNotes.isEmpty()) {
            // Devolver versión resumida de las notas (primeros 50 caracteres o primera frase)
            if (statusNotes.length() > 50) {
                return statusNotes.substring(0, 47) + "...";
            }
            return statusNotes;
        }

        if (event.get("additionalDetails") != null && event.get("additionalDetails") instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> additionalDetails = (Map<String, Object>) event.get("additionalDetails");
            if (additionalDetails.containsKey("rejectionReason")) {
                return additionalDetails.get("rejectionReason").toString();
            }
        }

        return "Fuera de cobertura"; // Razón genérica
    }
}
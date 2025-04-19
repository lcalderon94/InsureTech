package com.insurtech.notification.event.consumer;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.time.LocalDate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PolicyEventConsumer extends BaseEventConsumer {

    @KafkaListener(topics = "policy.created", containerFactory = "kafkaListenerContainerFactory")
    public void consumePolicyCreated(Map<String, Object> event, Acknowledgment acknowledgment) {
        log.info("Recibido evento de póliza creada: {}", event.get("policyNumber"));
        processEvent(event, acknowledgment);
    }

    @KafkaListener(topics = "policy.updated", containerFactory = "kafkaListenerContainerFactory")
    public void consumePolicyUpdated(Map<String, Object> event, Acknowledgment acknowledgment) {
        log.info("Recibido evento de póliza actualizada: {}", event.get("policyNumber"));
        processEvent(event, acknowledgment);
    }

    @KafkaListener(topics = "policy.status.changed", containerFactory = "kafkaListenerContainerFactory")
    public void consumePolicyStatusChanged(Map<String, Object> event, Acknowledgment acknowledgment) {
        log.info("Recibido evento de cambio de estado de póliza: {}", event.get("policyNumber"));
        processEvent(event, acknowledgment);
    }

    @KafkaListener(topics = "policy.cancelled", containerFactory = "kafkaListenerContainerFactory")
    public void consumePolicyCancelled(Map<String, Object> event, Acknowledgment acknowledgment) {
        log.info("Recibido evento de cancelación de póliza: {}", event.get("policyNumber"));
        processEvent(event, acknowledgment);
    }

    @KafkaListener(topics = "policy.renewed", containerFactory = "kafkaListenerContainerFactory")
    public void consumePolicyRenewed(Map<String, Object> event, Acknowledgment acknowledgment) {
        log.info("Recibido evento de renovación de póliza: {}", event.get("policyNumber"));
        processEvent(event, acknowledgment);
    }

    @Override
    protected String determineEventType(Map<String, Object> event) {
        // Caso especial para POLICY_CREATED
        if (event.get("eventType") != null) {
            String rawType = event.get("eventType").toString();
            if (rawType.equals("POLICY_CREATED")) {
                return "policy.created";
            }
        }

        // Determinar el tipo de evento basado en el tópico Kafka o en los datos del evento
        if (event.get("actionType") != null) {
            return "policy." + event.get("actionType").toString().toLowerCase();
        }

        // Usar el eventType directamente del evento si está disponible
        if (event.get("eventType") != null) {
            return event.get("eventType").toString().toLowerCase();
        }

        // Valor por defecto basado en la presencia de ciertos campos
        if (event.get("policyStatus") != null && event.get("additionalDetails") != null
                && ((Map<String, Object>)event.get("additionalDetails")).containsKey("previousStatus")) {
            return "policy.status.changed";
        }

        return "policy.event"; // Último recurso
    }

    @Override
    protected Map<String, Object> extractVariables(Map<String, Object> event) {
        Map<String, Object> variables = new HashMap<>(event);

        // Formatear fechas
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        if (event.get("effectiveDate") != null) {
            try {
                LocalDate effectiveDate = parseDate(event.get("effectiveDate"));
                variables.put("effectiveDate", effectiveDate.format(dateFormatter));
            } catch (Exception e) {
                log.warn("Error parseando effectiveDate: {}", e.getMessage());
            }
        }

        if (event.get("expirationDate") != null) {
            try {
                LocalDate expirationDate = parseDate(event.get("expirationDate"));
                variables.put("expirationDate", expirationDate.format(dateFormatter));

                // Para vencimiento próximo
                long daysUntil = java.time.temporal.ChronoUnit.DAYS.between(
                        LocalDate.now(), expirationDate);
                variables.put("daysUntilExpiration", String.valueOf(daysUntil));
            } catch (Exception e) {
                log.warn("Error parseando expirationDate: {}", e.getMessage());
            }
        }

        // Datos de cambio de estado
        if (event.get("policyStatus") != null) {
            variables.put("newStatus", event.get("policyStatus"));
            variables.put("statusChangeImpact", getStatusChangeImpact(event.get("policyStatus").toString()));
        }

        // Datos de cancelación
        if (event.get("actionType") != null && "CANCELLED".equals(event.get("actionType"))) {
            variables.put("cancellationDate", LocalDate.now().format(dateFormatter));
            variables.put("refundInfoShort", getRefundInfo(event));
        }

        return variables;
    }

    @Override
    protected String extractEmail(Map<String, Object> event) {
        if (event.get("customerEmail") != null && !event.get("customerEmail").toString().trim().isEmpty()) {
            return event.get("customerEmail").toString();
        }

        if (event.get("additionalDetails") != null) {
            Map<String, Object> details = (Map<String, Object>) event.get("additionalDetails");
            if (details.get("customerEmail") != null && !details.get("customerEmail").toString().trim().isEmpty()) {
                return details.get("customerEmail").toString();
            }
        }

        return null;
    }

    @Override
    protected String extractPhone(Map<String, Object> event) {
        if (event.get("customerPhone") != null && !event.get("customerPhone").toString().trim().isEmpty()) {
            return event.get("customerPhone").toString();
        }

        if (event.get("additionalDetails") != null) {
            Map<String, Object> details = (Map<String, Object>) event.get("additionalDetails");
            if (details.get("customerPhone") != null && !details.get("customerPhone").toString().trim().isEmpty()) {
                return details.get("customerPhone").toString();
            }
        }

        return null;
    }

    // Métodos auxiliares
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

    private String getStatusChangeImpact(String newStatus) {
        switch (newStatus) {
            case "ACTIVE": return "Su cobertura está ahora activa.";
            case "SUSPENDED": return "Su cobertura está temporalmente suspendida.";
            case "CANCELLED": return "Su cobertura ha finalizado.";
            case "PENDING_PAYMENT": return "Pendiente de pago para activar cobertura.";
            default: return "Por favor revise detalles en su área personal.";
        }
    }

    private String getRefundInfo(Map<String, Object> event) {
        if (event.get("additionalDetails") != null) {
            Map<String, Object> details = (Map<String, Object>) event.get("additionalDetails");
            if (details.get("refundAmount") != null) {
                return "Reembolso: " + details.get("refundAmount") + "€.";
            }
        }
        return "Consulte detalles de reembolso en su área personal.";
    }
}
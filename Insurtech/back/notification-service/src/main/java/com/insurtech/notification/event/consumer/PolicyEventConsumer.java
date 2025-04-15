package com.insurtech.notification.event.consumer;

import com.insurtech.notification.event.model.PolicyEvent;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PolicyEventConsumer extends BaseEventConsumer<PolicyEvent> {

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

    @KafkaListener(topics = "policy.cancelled", containerFactory = "policyKafkaListenerContainerFactory")
    public void consumePolicyCancelled(PolicyEvent event, Acknowledgment acknowledgment) {
        log.info("Recibido evento de cancelación de póliza: {}", event.getPolicyNumber());
        processEvent(event, acknowledgment);
    }

    @KafkaListener(topics = "policy.renewed", containerFactory = "policyKafkaListenerContainerFactory")
    public void consumePolicyRenewed(PolicyEvent event, Acknowledgment acknowledgment) {
        log.info("Recibido evento de renovación de póliza: {}", event.getPolicyNumber());
        processEvent(event, acknowledgment);
    }

    @Override
    protected String determineEventType(PolicyEvent event) {
        // Determinar el tipo de evento basado en el tópico Kafka o en los datos del evento
        if (event.getActionType() != null) {
            return "policy." + event.getActionType().toLowerCase();
        }

        // Usar el eventType directamente del evento si está disponible
        if (event.getEventType() != null) {
            return event.getEventType();
        }

        // Valor por defecto basado en la presencia de ciertos campos
        if (event.getPolicyStatus() != null && event.getAdditionalDetails() != null
                && event.getAdditionalDetails().containsKey("previousStatus")) {
            return "policy.status.changed";
        }

        return "policy.event"; // Último recurso
    }

    @Override
    protected Map<String, Object> extractVariables(PolicyEvent event) {
        Map<String, Object> variables = new HashMap<>();

        // Datos básicos de póliza
        variables.put("policyNumber", event.getPolicyNumber());
        variables.put("policyType", event.getPolicyType());
        variables.put("customerName", event.getCustomerName());

        // Formatear fechas
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        if (event.getEffectiveDate() != null) {
            variables.put("effectiveDate", event.getEffectiveDate().format(dateFormatter));
        }
        if (event.getExpirationDate() != null) {
            variables.put("expirationDate", event.getExpirationDate().format(dateFormatter));
        }

        // Datos de renovación
        if (event.getActionType() != null && event.getActionType().equals("RENEWED")) {
            variables.put("newEffectiveDate", event.getEffectiveDate().format(dateFormatter));
            variables.put("newExpirationDate", event.getExpirationDate().format(dateFormatter));
            if (event.getAdditionalDetails() != null && event.getAdditionalDetails().containsKey("newPremium")) {
                variables.put("newPremium", event.getAdditionalDetails().get("newPremium"));
            }
        }

        // Datos de cambio de estado
        if (event.getPolicyStatus() != null) {
            variables.put("newStatus", event.getPolicyStatus());
            if (event.getAdditionalDetails() != null && event.getAdditionalDetails().containsKey("previousStatus")) {
                variables.put("previousStatus", event.getAdditionalDetails().get("previousStatus"));
            }
            variables.put("statusChangeImpact", getStatusChangeImpact(event.getPolicyStatus()));
        }

        // Datos de cancelación
        if (event.getActionType() != null && event.getActionType().equals("CANCELLED")) {
            variables.put("cancellationDate", getCurrentDate(dateFormatter));
            variables.put("coverageEndDate", event.getExpirationDate().format(dateFormatter));
            variables.put("refundInfoShort", getRefundInfo(event));
        }

        // Para vencimiento próximo
        if (event.getExpirationDate() != null) {
            long daysUntil = java.time.temporal.ChronoUnit.DAYS.between(
                    java.time.LocalDate.now(), event.getExpirationDate());
            variables.put("daysUntilExpiration", String.valueOf(daysUntil));
        }

        // Prima
        if (event.getAdditionalDetails() != null && event.getAdditionalDetails().containsKey("premium")) {
            variables.put("premium", event.getAdditionalDetails().get("premium"));
        }

        // Incluir datos adicionales
        if (event.getAdditionalDetails() != null) {
            variables.putAll(event.getAdditionalDetails());
        }

        return variables;
    }

    @Override
    protected String extractEmail(PolicyEvent event) {
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
    protected String extractPhone(PolicyEvent event) {
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

    private String getStatusChangeImpact(String newStatus) {
        switch (newStatus) {
            case "ACTIVE": return "Su cobertura está ahora activa.";
            case "SUSPENDED": return "Su cobertura está temporalmente suspendida.";
            case "CANCELLED": return "Su cobertura ha finalizado.";
            case "PENDING_PAYMENT": return "Pendiente de pago para activar cobertura.";
            default: return "Por favor revise detalles en su área personal.";
        }
    }

    private String getRefundInfo(PolicyEvent event) {
        if (event.getAdditionalDetails() != null && event.getAdditionalDetails().containsKey("refundAmount")) {
            return "Reembolso: " + event.getAdditionalDetails().get("refundAmount") + "€.";
        }
        return "Consulte detalles de reembolso en su área personal.";
    }

    private String getCurrentDate(DateTimeFormatter formatter) {
        return java.time.LocalDate.now().format(formatter);
    }
}
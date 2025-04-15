package com.insurtech.notification.event.consumer;

import com.insurtech.notification.event.model.CustomerEvent;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CustomerEventConsumer extends BaseEventConsumer<CustomerEvent> {

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

    @KafkaListener(topics = "customer.password.reset", containerFactory = "customerKafkaListenerContainerFactory")
    public void consumePasswordReset(CustomerEvent event, Acknowledgment acknowledgment) {
        log.info("Recibido evento de restablecimiento de contraseña: {}", event.getDocumentNumber());
        processEvent(event, acknowledgment);
    }

    @KafkaListener(topics = "customer.security.alert", containerFactory = "customerKafkaListenerContainerFactory")
    public void consumeSecurityAlert(CustomerEvent event, Acknowledgment acknowledgment) {
        log.info("Recibido evento de alerta de seguridad: {}", event.getDocumentNumber());
        processEvent(event, acknowledgment);
    }

    @Override
    protected String determineEventType(CustomerEvent event) {
        if (event.getEventType() != null) {
            return event.getEventType();
        }

        // Determinar por acción
        if (event.getActionType() != null) {
            switch (event.getActionType()) {
                case "CREATED": return "customer.created";
                case "UPDATED": return "customer.updated";
                case "PASSWORD_RESET": return "customer.password.reset";
                case "LOGIN_ATTEMPT": return "customer.security.alert";
                case "VERIFIED":
                case "DEACTIVATED":
                    return "customer.status.changed";
            }
        }

        // Determinar por cambio de estado
        if (event.getCustomerStatus() != null) {
            return "customer.status.changed";
        }

        return "customer.event"; // Valor por defecto
    }

    @Override
    protected Map<String, Object> extractVariables(CustomerEvent event) {
        Map<String, Object> variables = new HashMap<>();

        // Datos básicos
        variables.put("customerName", event.getFirstName() + " " + event.getLastName());
        variables.put("firstName", event.getFirstName());
        variables.put("lastName", event.getLastName());
        variables.put("documentNumber", event.getDocumentNumber());
        variables.put("email", event.getEmail());
        variables.put("phone", event.getPhone());

        // Datos de dirección
        if (event.getAddress() != null) {
            variables.put("address", event.getAddress());
        }
        if (event.getCity() != null) {
            variables.put("city", event.getCity());
        }
        if (event.getZipCode() != null) {
            variables.put("zipCode", event.getZipCode());
        }
        if (event.getCountry() != null) {
            variables.put("country", event.getCountry());
        }

        // Estado
        if (event.getCustomerStatus() != null) {
            variables.put("status", event.getCustomerStatus());
        }

        // Acción
        if (event.getActionType() != null) {
            variables.put("actionType", event.getActionType());
        }

        // Fecha de registro para nuevos clientes
        if ("CREATED".equals(event.getActionType())) {
            variables.put("registrationDate", formatDate(java.time.LocalDate.now()));
            variables.put("customerId", event.getCustomerId().toString());
            variables.put("username", event.getEmail()); // Por defecto
        }

        // Variables para restablecimiento de contraseña
        if ("PASSWORD_RESET".equals(event.getActionType())) {
            if (event.getAdditionalDetails() != null && event.getAdditionalDetails().containsKey("resetCode")) {
                variables.put("resetCode", event.getAdditionalDetails().get("resetCode"));
            } else {
                variables.put("resetCode", generateRandomCode(6));
            }
            variables.put("expiryMinutes", "30"); // O el valor que corresponda
        }

        // Variables para alerta de seguridad
        if ("LOGIN_ATTEMPT".equals(event.getActionType()) ||
                (event.getEventType() != null && event.getEventType().contains("security"))) {
            variables.put("location", getLocationInfo(event));
        }

        // Cambio de estado
        if ("status.changed".equals(event.getEventType()) ||
                event.getCustomerStatus() != null) {
            variables.put("oldStatus", getOldStatus(event));
            variables.put("newStatus", event.getCustomerStatus());
            variables.put("statusChangeDate", formatDate(java.time.LocalDate.now()));
            variables.put("statusChangeReason", getStatusChangeReason(event));
            variables.put("statusChangeImpact", getStatusChangeImpact(event.getCustomerStatus()));
        }

        // Variables para cambios en cliente
        if ("UPDATED".equals(event.getActionType())) {
            variables.put("updateDate", formatDate(java.time.LocalDate.now()));
            variables.put("updatedBy", "Sistema" ); // O el valor que corresponda
            variables.put("changesList", getChangesList(event));
        }

        // Agregar detalles adicionales
        if (event.getAdditionalDetails() != null) {
            variables.putAll(event.getAdditionalDetails());
        }

        return variables;
    }

    @Override
    protected String extractEmail(CustomerEvent event) {
        if (event.getEmail() != null && !event.getEmail().trim().isEmpty()) {
            return event.getEmail();
        }

        if (event.getAdditionalDetails() != null) {
            Object email = event.getAdditionalDetails().get("email");
            if (email != null && !email.toString().trim().isEmpty()) {
                return email.toString();
            }
        }

        return null;
    }

    @Override
    protected String extractPhone(CustomerEvent event) {
        if (event.getPhone() != null && !event.getPhone().trim().isEmpty()) {
            return event.getPhone();
        }

        if (event.getAdditionalDetails() != null) {
            Object phone = event.getAdditionalDetails().get("phone");
            if (phone != null && !phone.toString().trim().isEmpty()) {
                return phone.toString();
            }
        }

        return null;
    }

    // Métodos auxiliares

    private String formatDate(java.time.LocalDate date) {
        if (date == null) {
            return "";
        }
        return date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private String generateRandomCode(int length) {
        // Generar código aleatorio numérico
        StringBuilder code = new StringBuilder();
        java.util.Random random = new java.util.Random();
        for (int i = 0; i < length; i++) {
            code.append(random.nextInt(10));
        }
        return code.toString();
    }

    private String getLocationInfo(CustomerEvent event) {
        if (event.getAdditionalDetails() != null) {
            if (event.getAdditionalDetails().containsKey("ipLocation")) {
                return event.getAdditionalDetails().get("ipLocation").toString();
            }
            if (event.getAdditionalDetails().containsKey("location")) {
                return event.getAdditionalDetails().get("location").toString();
            }
        }
        return "ubicación desconocida";
    }

    private String getOldStatus(CustomerEvent event) {
        if (event.getAdditionalDetails() != null && event.getAdditionalDetails().containsKey("previousStatus")) {
            return event.getAdditionalDetails().get("previousStatus").toString();
        }
        return "estado anterior";
    }

    private String getStatusChangeReason(CustomerEvent event) {
        if (event.getAdditionalDetails() != null && event.getAdditionalDetails().containsKey("statusChangeReason")) {
            return event.getAdditionalDetails().get("statusChangeReason").toString();
        }
        return "actualización de sistema";
    }

    private String getStatusChangeImpact(String newStatus) {
        if (newStatus == null) {
            return "Por favor contacte con atención al cliente para más información.";
        }

        switch (newStatus.toUpperCase()) {
            case "ACTIVE": return "Su cuenta está completamente activa.";
            case "INACTIVE": return "Su cuenta ha sido desactivada temporalmente.";
            case "PENDING_VERIFICATION": return "Su cuenta requiere verificación.";
            case "BLOCKED": return "Su cuenta ha sido bloqueada por motivos de seguridad.";
            default: return "Por favor revise su área personal para más detalles.";
        }
    }

    private String getChangesList(CustomerEvent event) {
        if (event.getAdditionalDetails() != null && event.getAdditionalDetails().containsKey("changedFields")) {
            return event.getAdditionalDetails().get("changedFields").toString();
        }
        return "actualización de datos de perfil";
    }
}
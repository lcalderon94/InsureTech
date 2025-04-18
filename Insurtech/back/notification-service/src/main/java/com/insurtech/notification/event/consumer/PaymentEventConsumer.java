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
public class PaymentEventConsumer extends BaseEventConsumer {

    @KafkaListener(topics = "payment.created", containerFactory = "kafkaListenerContainerFactory")
    public void consumePaymentCreated(Map<String, Object> event, Acknowledgment acknowledgment) {
        log.info("Recibido evento de pago creado: {}", event.get("paymentReference"));
        processEvent(event, acknowledgment);
    }

    @KafkaListener(topics = "payment.processed", containerFactory = "kafkaListenerContainerFactory")
    public void consumePaymentProcessed(Map<String, Object> event, Acknowledgment acknowledgment) {
        log.info("Recibido evento de pago procesado: {}", event.get("paymentReference"));
        processEvent(event, acknowledgment);
    }

    @KafkaListener(topics = "payment.failed", containerFactory = "kafkaListenerContainerFactory")
    public void consumePaymentFailed(Map<String, Object> event, Acknowledgment acknowledgment) {
        log.info("Recibido evento de pago fallido: {}", event.get("paymentReference"));
        processEvent(event, acknowledgment);
    }

    @KafkaListener(topics = "payment.refund.processed", containerFactory = "kafkaListenerContainerFactory")
    public void consumeRefundProcessed(Map<String, Object> event, Acknowledgment acknowledgment) {
        log.info("Recibido evento de reembolso procesado: {}", event.get("paymentReference"));
        processEvent(event, acknowledgment);
    }

    @KafkaListener(topics = "payment.reminder", containerFactory = "kafkaListenerContainerFactory")
    public void consumePaymentReminder(Map<String, Object> event, Acknowledgment acknowledgment) {
        log.info("Recibido evento de recordatorio de pago: {}", event.get("paymentReference"));
        processEvent(event, acknowledgment);
    }

    @Override
    protected String determineEventType(Map<String, Object> event) {
        if (event.get("eventType") != null) {
            return event.get("eventType").toString();
        }

        // Determinar por estado
        if (event.get("paymentStatus") != null) {
            String status = event.get("paymentStatus").toString();
            switch (status) {
                case "SUCCESSFUL": return "payment.processed";
                case "FAILED": return "payment.failed";
                case "PENDING": return "payment.created";
                case "REFUNDED": return "payment.refund.processed";
                case "OVERDUE": return "payment.reminder";
            }
        }

        // Determinar por descriptor
        if (event.get("description") != null) {
            String description = event.get("description").toString().toLowerCase();
            if (description.contains("refund")) {
                return "payment.refund.processed";
            }
            if (description.contains("reminder")) {
                return "payment.reminder";
            }
        }

        return "payment.event"; // Valor por defecto
    }

    @Override
    protected Map<String, Object> extractVariables(Map<String, Object> event) {
        Map<String, Object> variables = new HashMap<>();

        // Datos básicos
        variables.put("paymentReference", getString(event, "paymentReference"));
        variables.put("policyNumber", getString(event, "policyNumber"));
        variables.put("customerName", getString(event, "customerName"));

        // Importe y moneda
        Object amountObj = event.get("amount");
        if (amountObj != null) {
            String formattedAmount = formatCurrency(amountObj);
            variables.put("paymentAmount", formattedAmount);
            variables.put("amount", formattedAmount); // Alias para compatibilidad
            variables.put("refundAmount", formattedAmount); // Para reembolsos
            variables.put("amountDue", formattedAmount); // Para recordatorios
        }
        variables.put("currency", event.get("currency") != null ? event.get("currency").toString() : "EUR");

        // Método de pago
        variables.put("paymentMethod", formatPaymentMethod(getString(event, "paymentMethod")));

        // Formatear fechas
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        Object paymentDateObj = event.get("paymentDate");
        if (paymentDateObj != null) {
            LocalDate paymentDate = parseDate(paymentDateObj);
            variables.put("paymentDate", paymentDate.format(dateFormatter));
            variables.put("processedDate", paymentDate.format(dateFormatter)); // Alias
        } else {
            variables.put("processedDate", LocalDate.now().format(dateFormatter));
        }

        Object dueDateObj = event.get("dueDate");
        if (dueDateObj != null) {
            variables.put("dueDate", parseDate(dueDateObj).format(dateFormatter));
        }

        // Datos específicos para pagos fallidos
        if ("FAILED".equals(getString(event, "paymentStatus"))) {
            variables.put("attemptDate", LocalDate.now().format(dateFormatter));
            variables.put("failureReason", getFailureReason(event));
            // Calcular fecha límite de período de gracia (ejemplo: 5 días)
            variables.put("gracePeriodDate", LocalDate.now().plusDays(5).format(dateFormatter));
        }

        // Datos específicos para reembolsos
        if ("REFUNDED".equals(getString(event, "paymentStatus")) ||
                (event.get("eventType") != null && event.get("eventType").toString().contains("refund"))) {
            variables.put("refundTimeframeDays", "3-5"); // O un valor dinámico
        }

        // Datos para recordatorios
        if ("OVERDUE".equals(getString(event, "paymentStatus")) ||
                (event.get("eventType") != null && event.get("eventType").toString().contains("reminder"))) {
            // ID para enlace de pago
            if (event.get("paymentId") != null) {
                variables.put("paymentId", event.get("paymentId").toString());
            }
        }

        // Descripción
        variables.put("description", getString(event, "description"));

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

    private String formatPaymentMethod(String method) {
        if (method == null) {
            return "tarjeta";
        }

        switch (method.toUpperCase()) {
            case "CREDIT_CARD": return "tarjeta de crédito";
            case "DEBIT_CARD": return "tarjeta de débito";
            case "BANK_TRANSFER": return "transferencia bancaria";
            case "DIRECT_DEBIT": return "domiciliación bancaria";
            default: return method.toLowerCase();
        }
    }

    private String getFailureReason(Map<String, Object> event) {
        String description = getString(event, "description");
        if (description != null && !description.isEmpty()) {
            return description;
        }

        if (event.get("additionalDetails") != null && event.get("additionalDetails") instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> additionalDetails = (Map<String, Object>) event.get("additionalDetails");
            if (additionalDetails.containsKey("failureReason")) {
                return additionalDetails.get("failureReason").toString();
            }
        }

        return "problemas con el método de pago"; // Valor genérico
    }
}
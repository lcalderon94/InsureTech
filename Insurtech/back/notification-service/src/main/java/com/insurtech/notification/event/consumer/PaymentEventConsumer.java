package com.insurtech.notification.event.consumer;

import com.insurtech.notification.event.model.PaymentEvent;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PaymentEventConsumer extends BaseEventConsumer<PaymentEvent> {

    @KafkaListener(topics = "payment.created", containerFactory = "paymentKafkaListenerContainerFactory")
    public void consumePaymentCreated(PaymentEvent event, Acknowledgment acknowledgment) {
        log.info("Recibido evento de pago creado: {}", event.getPaymentReference());
        processEvent(event, acknowledgment);
    }

    @KafkaListener(topics = "payment.processed", containerFactory = "paymentKafkaListenerContainerFactory")
    public void consumePaymentProcessed(PaymentEvent event, Acknowledgment acknowledgment) {
        log.info("Recibido evento de pago procesado: {}", event.getPaymentReference());
        processEvent(event, acknowledgment);
    }

    @KafkaListener(topics = "payment.failed", containerFactory = "paymentKafkaListenerContainerFactory")
    public void consumePaymentFailed(PaymentEvent event, Acknowledgment acknowledgment) {
        log.info("Recibido evento de pago fallido: {}", event.getPaymentReference());
        processEvent(event, acknowledgment);
    }

    @KafkaListener(topics = "payment.refund.processed", containerFactory = "paymentKafkaListenerContainerFactory")
    public void consumeRefundProcessed(PaymentEvent event, Acknowledgment acknowledgment) {
        log.info("Recibido evento de reembolso procesado: {}", event.getPaymentReference());
        processEvent(event, acknowledgment);
    }

    @KafkaListener(topics = "payment.reminder", containerFactory = "paymentKafkaListenerContainerFactory")
    public void consumePaymentReminder(PaymentEvent event, Acknowledgment acknowledgment) {
        log.info("Recibido evento de recordatorio de pago: {}", event.getPaymentReference());
        processEvent(event, acknowledgment);
    }

    @Override
    protected String determineEventType(PaymentEvent event) {
        if (event.getEventType() != null) {
            return event.getEventType();
        }

        // Determinar por estado
        if (event.getPaymentStatus() != null) {
            switch (event.getPaymentStatus()) {
                case "SUCCESSFUL": return "payment.processed";
                case "FAILED": return "payment.failed";
                case "PENDING": return "payment.created";
                case "REFUNDED": return "payment.refund.processed";
                case "OVERDUE": return "payment.reminder";
            }
        }

        // Determinar por descriptor
        if (event.getDescription() != null) {
            if (event.getDescription().toLowerCase().contains("refund")) {
                return "payment.refund.processed";
            }
            if (event.getDescription().toLowerCase().contains("reminder")) {
                return "payment.reminder";
            }
        }

        return "payment.event"; // Valor por defecto
    }

    @Override
    protected Map<String, Object> extractVariables(PaymentEvent event) {
        Map<String, Object> variables = new HashMap<>();

        // Datos básicos
        variables.put("paymentReference", event.getPaymentReference());
        variables.put("policyNumber", event.getPolicyNumber());
        variables.put("customerName", event.getCustomerName());

        // Importe y moneda
        if (event.getAmount() != null) {
            variables.put("paymentAmount", formatCurrency(event.getAmount()));
            variables.put("amount", formatCurrency(event.getAmount())); // Alias para compatibilidad
            variables.put("refundAmount", formatCurrency(event.getAmount())); // Para reembolsos
            variables.put("amountDue", formatCurrency(event.getAmount())); // Para recordatorios
        }
        variables.put("currency", event.getCurrency() != null ? event.getCurrency() : "EUR");

        // Método de pago
        variables.put("paymentMethod", formatPaymentMethod(event.getPaymentMethod()));

        // Formatear fechas
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        if (event.getPaymentDate() != null) {
            variables.put("paymentDate", event.getPaymentDate().format(dateFormatter));
            variables.put("processedDate", event.getPaymentDate().format(dateFormatter)); // Alias
        } else {
            variables.put("processedDate", java.time.LocalDate.now().format(dateFormatter));
        }

        if (event.getDueDate() != null) {
            variables.put("dueDate", event.getDueDate().format(dateFormatter));
        }

        // Datos específicos para pagos fallidos
        if ("FAILED".equals(event.getPaymentStatus())) {
            variables.put("attemptDate", java.time.LocalDate.now().format(dateFormatter));
            variables.put("failureReason", getFailureReason(event));
            // Calcular fecha límite de período de gracia (ejemplo: 5 días)
            variables.put("gracePeriodDate", java.time.LocalDate.now().plusDays(5).format(dateFormatter));
        }

        // Datos específicos para reembolsos
        if ("REFUNDED".equals(event.getPaymentStatus()) ||
                (event.getEventType() != null && event.getEventType().contains("refund"))) {
            variables.put("refundTimeframeDays", "3-5"); // O un valor dinámico
        }

        // Datos para recordatorios
        if ("OVERDUE".equals(event.getPaymentStatus()) ||
                (event.getEventType() != null && event.getEventType().contains("reminder"))) {
            // ID para enlace de pago
            variables.put("paymentId", event.getPaymentId().toString());
        }

        // Descripción
        if (event.getDescription() != null) {
            variables.put("description", event.getDescription());
        }

        // Agregar detalles adicionales
        if (event.getAdditionalDetails() != null) {
            variables.putAll(event.getAdditionalDetails());
        }

        return variables;
    }

    @Override
    protected String extractEmail(PaymentEvent event) {
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
    protected String extractPhone(PaymentEvent event) {
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

    private String formatCurrency(java.math.BigDecimal amount) {
        if (amount == null) {
            return "0.00";
        }
        return amount.setScale(2, java.math.RoundingMode.HALF_UP).toString();
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

    private String getFailureReason(PaymentEvent event) {
        if (event.getDescription() != null && !event.getDescription().isEmpty()) {
            return event.getDescription();
        }

        if (event.getAdditionalDetails() != null && event.getAdditionalDetails().containsKey("failureReason")) {
            return event.getAdditionalDetails().get("failureReason").toString();
        }

        return "problemas con el método de pago"; // Valor genérico
    }
}
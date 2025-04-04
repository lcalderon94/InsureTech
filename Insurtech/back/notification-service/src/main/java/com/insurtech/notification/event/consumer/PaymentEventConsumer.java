package com.insurtech.notification.event.consumer;

import com.insurtech.notification.event.model.PaymentEvent;
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
import java.util.Map;
import java.util.Optional;

@Component
@Slf4j
public class PaymentEventConsumer extends BaseEventConsumer<PaymentEvent> {

    // Escuchar en los tópicos de Payment utilizando formato con punto
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

    @Override
    protected void processEventInternal(PaymentEvent event) throws NotificationException {
        log.info("Procesando evento de pago: {} - {}", event.getPaymentReference(), event.getPaymentStatus());

        // Determinar la plantilla según el estado del pago
        String templateCode = determineTemplateCode(event);
        if (templateCode == null) {
            log.info("No se requiere notificación para este evento de pago: {}", event.getPaymentStatus());
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

        // Enviar SMS para pagos exitosos y fallidos
        if (shouldSendSms(event) && event.getCustomerPhone() != null && !event.getCustomerPhone().isBlank()) {
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

    private String determineTemplateCode(PaymentEvent event) {
        switch (event.getPaymentStatus()) {
            case "SUCCESSFUL":
                return "payment_confirmation";
            case "FAILED":
                return "payment_failed";
            case "PENDING":
                return "payment_pending";
            case "OVERDUE":
                return "payment_overdue";
            case "REFUNDED":
                return "payment_refunded";
            default:
                return null;
        }
    }

    private Map<String, Object> prepareTemplateData(PaymentEvent event) {
        Map<String, Object> data = new HashMap<>();
        data.put("paymentReference", event.getPaymentReference());
        data.put("policyNumber", event.getPolicyNumber());
        data.put("customerName", event.getCustomerName());
        data.put("amount", event.getAmount());
        data.put("currency", event.getCurrency());
        data.put("paymentMethod", event.getPaymentMethod());
        data.put("paymentDate", event.getPaymentDate());
        data.put("dueDate", event.getDueDate());
        data.put("status", event.getPaymentStatus());
        data.put("description", event.getDescription());

        // Calcular días de retraso si es una factura vencida
        if ("OVERDUE".equals(event.getPaymentStatus()) && event.getDueDate() != null) {
            long daysOverdue = ChronoUnit.DAYS.between(event.getDueDate(), LocalDate.now());
            data.put("daysOverdue", daysOverdue);
        }

        // Agregar detalles adicionales si existen
        if (event.getAdditionalDetails() != null) {
            data.putAll(event.getAdditionalDetails());
        }

        return data;
    }

    private NotificationPriority determinePriority(PaymentEvent event) {
        switch (event.getPaymentStatus()) {
            case "FAILED":
            case "OVERDUE":
                return NotificationPriority.HIGH;
            case "SUCCESSFUL":
            case "REFUNDED":
                return NotificationPriority.MEDIUM;
            default:
                return NotificationPriority.LOW;
        }
    }

    private boolean shouldSendSms(PaymentEvent event) {
        return "SUCCESSFUL".equals(event.getPaymentStatus()) ||
                "FAILED".equals(event.getPaymentStatus()) ||
                "OVERDUE".equals(event.getPaymentStatus());
    }
}
package com.insurtech.payment.event;

import com.insurtech.payment.client.CustomerServiceClient;
import com.insurtech.payment.model.dto.PaymentDto;
import com.insurtech.payment.model.entity.Payment;
import com.insurtech.payment.model.event.PaymentProcessedEvent;
import com.insurtech.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true")
public class PaymentEventHandler {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final CustomerServiceClient customerServiceClient;
    private final PaymentService paymentService;

    /**
     * Publica evento de notificación cuando un pago es procesado
     */
    public void handlePaymentProcessed(PaymentProcessedEvent event) {
        log.info("Manejando evento de pago procesado: {}", event.getPaymentNumber());

        try {
            // Crear notificación para el cliente
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "PAYMENT_STATUS");
            notification.put("customerNumber", event.getCustomerNumber());

            String title;
            String message;

            if (event.isSuccessful()) {
                title = "Pago procesado exitosamente";
                message = String.format("Su pago por %s %s ha sido procesado exitosamente.",
                        event.getAmount(), event.getCurrency());
            } else {
                title = "Problema con su pago";
                message = String.format("Hubo un problema al procesar su pago por %s %s. Motivo: %s",
                        event.getAmount(), event.getCurrency(), event.getErrorMessage());
            }

            notification.put("title", title);
            notification.put("message", message);
            notification.put("paymentNumber", event.getPaymentNumber());

            // Enviar notificación al cliente
            customerServiceClient.sendNotification("PAYMENT-" + UUID.randomUUID().toString(), notification);

            // También publicar en el topic de notificaciones para otros microservicios
            kafkaTemplate.send("payment.notifications", event.getCustomerNumber(), notification);

        } catch (Exception e) {
            log.error("Error al manejar evento de pago procesado: {}", e.getMessage());
        }
    }

    /**
     * Escucha eventos de cambio de estado de póliza
     */
    @KafkaListener(topics = "policy.status.changed", groupId = "payment-service")
    public void consumePolicyStatusChangedEvent(Map<String, Object> event) {
        log.info("Recibido evento de cambio de estado de póliza");

        try {
            String policyNumber = (String) event.get("policyNumber");
            String newStatus = (String) event.get("newStatus");

            // Si la póliza fue cancelada, marcar sus pagos pendientes como cancelados
            if ("CANCELLED".equals(newStatus)) {
                log.info("Póliza {} cancelada. Procesando pagos pendientes.", policyNumber);

                List<PaymentDto> pendingPayments = paymentService.getPaymentsByPolicyNumber(policyNumber)
                        .stream()
                        .filter(p -> p.getStatus() == Payment.PaymentStatus.PENDING)
                        .collect(Collectors.toList());

                for (PaymentDto payment : pendingPayments) {
                    paymentService.cancelPayment(payment.getPaymentNumber(), "Póliza cancelada");
                }

                log.info("Cancelados {} pagos pendientes de la póliza {}", pendingPayments.size(), policyNumber);
            }

        } catch (Exception e) {
            log.error("Error al procesar evento de cambio de estado de póliza: {}", e.getMessage());
        }
    }
}
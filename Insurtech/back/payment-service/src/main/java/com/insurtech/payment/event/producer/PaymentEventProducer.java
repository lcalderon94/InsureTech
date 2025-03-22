package com.insurtech.payment.event.producer;

import com.insurtech.payment.model.dto.PaymentDto;
import com.insurtech.payment.model.dto.RefundDto;
import com.insurtech.payment.model.event.PaymentCreatedEvent;
import com.insurtech.payment.model.event.PaymentFailedEvent;
import com.insurtech.payment.model.event.PaymentProcessedEvent;
import com.insurtech.payment.model.event.RefundProcessedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class PaymentEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String PAYMENT_CREATED_TOPIC = "payment.created";
    private static final String PAYMENT_PROCESSED_TOPIC = "payment.processed";
    private static final String PAYMENT_FAILED_TOPIC = "payment.failed";
    private static final String REFUND_PROCESSED_TOPIC = "payment.refund.processed";
    private static final String TRANSACTION_STARTED_TOPIC = "payment.transaction.started";
    private static final String TRANSACTION_COMPLETED_TOPIC = "payment.transaction.completed";
    private static final String TRANSACTION_FAILED_TOPIC = "payment.transaction.failed";

    public void publishPaymentCreatedEvent(PaymentDto paymentDto) {
        log.info("Publicando evento de pago creado para ID: {}", paymentDto.getId());

        PaymentCreatedEvent event = new PaymentCreatedEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setTimestamp(LocalDateTime.now());
        event.setPaymentId(paymentDto.getId());
        event.setPaymentNumber(paymentDto.getPaymentNumber());
        event.setAmount(paymentDto.getAmount());
        event.setCurrency(paymentDto.getCurrency());
        event.setPolicyNumber(paymentDto.getPolicyNumber());
        event.setClaimNumber(paymentDto.getClaimNumber());
        event.setCustomerNumber(paymentDto.getCustomerNumber());

        sendEvent(PAYMENT_CREATED_TOPIC, paymentDto.getPaymentNumber(), event);
    }

    public void publishPaymentProcessedEvent(PaymentDto paymentDto) {
        log.info("Publicando evento de pago procesado para ID: {}", paymentDto.getId());

        PaymentProcessedEvent event = new PaymentProcessedEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setTimestamp(LocalDateTime.now());
        event.setPaymentId(paymentDto.getId());
        event.setPaymentNumber(paymentDto.getPaymentNumber());
        event.setAmount(paymentDto.getAmount());
        event.setCurrency(paymentDto.getCurrency());
        event.setPolicyNumber(paymentDto.getPolicyNumber());
        event.setClaimNumber(paymentDto.getClaimNumber());
        event.setCustomerNumber(paymentDto.getCustomerNumber());
        event.setCompletionDate(paymentDto.getCompletionDate());

        sendEvent(PAYMENT_PROCESSED_TOPIC, paymentDto.getPaymentNumber(), event);
    }

    public void publishPaymentFailedEvent(PaymentDto paymentDto) {
        log.info("Publicando evento de pago fallido para ID: {}", paymentDto.getId());

        PaymentFailedEvent event = new PaymentFailedEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setTimestamp(LocalDateTime.now());
        event.setPaymentId(paymentDto.getId());
        event.setPaymentNumber(paymentDto.getPaymentNumber());
        event.setAmount(paymentDto.getAmount());
        event.setCurrency(paymentDto.getCurrency());
        event.setPolicyNumber(paymentDto.getPolicyNumber());
        event.setClaimNumber(paymentDto.getClaimNumber());
        event.setCustomerNumber(paymentDto.getCustomerNumber());
        event.setFailureReason(paymentDto.getFailureReason());

        sendEvent(PAYMENT_FAILED_TOPIC, paymentDto.getPaymentNumber(), event);
    }

    public void publishRefundProcessedEvent(RefundDto refundDto) {
        log.info("Publicando evento de reembolso procesado para ID: {}", refundDto.getId());

        RefundProcessedEvent event = new RefundProcessedEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setTimestamp(LocalDateTime.now());
        event.setRefundId(refundDto.getId());
        event.setRefundNumber(refundDto.getRefundNumber());
        event.setOriginalPaymentId(refundDto.getOriginalPaymentId());
        event.setAmount(refundDto.getAmount());
        event.setPolicyNumber(refundDto.getPolicyNumber());
        event.setClaimNumber(refundDto.getClaimNumber());
        event.setCustomerNumber(refundDto.getCustomerNumber());
        event.setCompletionDate(refundDto.getCompletionDate());

        sendEvent(REFUND_PROCESSED_TOPIC, refundDto.getRefundNumber(), event);
    }

    public void publishTransactionStartedEvent(String transactionId, PaymentDto paymentDto) {
        log.info("Publicando evento de transacción iniciada: {}", transactionId);

        // Crear objeto de evento con los datos de la transacción
        // El formato exacto dependerá de los requisitos específicos del sistema

        sendEvent(TRANSACTION_STARTED_TOPIC, transactionId, paymentDto);
    }

    public void publishTransactionCompletedEvent(String transactionId, PaymentDto paymentDto) {
        log.info("Publicando evento de transacción completada: {}", transactionId);

        // Crear objeto de evento con los datos de la transacción
        // El formato exacto dependerá de los requisitos específicos del sistema

        sendEvent(TRANSACTION_COMPLETED_TOPIC, transactionId, paymentDto);
    }

    public void publishTransactionFailedEvent(String transactionId, PaymentDto paymentDto, String errorMessage) {
        log.info("Publicando evento de transacción fallida: {}", transactionId);

        // Crear objeto de evento con los datos de la transacción y el error
        // El formato exacto dependerá de los requisitos específicos del sistema

        sendEvent(TRANSACTION_FAILED_TOPIC, transactionId, paymentDto);
    }

    private void sendEvent(String topic, String key, Object event) {
        try {
            ListenableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, key, event);

            future.addCallback(new ListenableFutureCallback<SendResult<String, Object>>() {
                @Override
                public void onSuccess(SendResult<String, Object> result) {
                    log.debug("Evento enviado a {} con clave {}: [{}]",
                            topic, key, result.getRecordMetadata().offset());
                }

                @Override
                public void onFailure(Throwable ex) {
                    log.error("Error al enviar evento a {} con clave {}: {}",
                            topic, key, ex.getMessage());
                }
            });
        } catch (Exception e) {
            log.error("Error al publicar evento en {}: {}", topic, e.getMessage());
        }
    }
}
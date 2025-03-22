package com.insurtech.payment.event.consumer;

import com.insurtech.payment.client.ClaimServiceClient;
import com.insurtech.payment.model.dto.PaymentDto;
import com.insurtech.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Slf4j
@RequiredArgsConstructor
public class ClaimPaymentConsumer {

    private final PaymentService paymentService;
    private final ClaimServiceClient claimServiceClient;

    @KafkaListener(topics = "claim.payment.requested", groupId = "payment-service")
    public void consumeClaimPaymentRequestedEvent(ClaimPaymentRequestedEvent event) {
        log.info("Recibido evento de solicitud de pago para reclamación: {}", event.getClaimNumber());

        try {
            // Crear un pago para la reclamación
            PaymentDto paymentDto = new PaymentDto();
            paymentDto.setClaimNumber(event.getClaimNumber());
            paymentDto.setPolicyNumber(event.getPolicyNumber());
            paymentDto.setCustomerNumber(event.getCustomerNumber());
            paymentDto.setAmount(event.getAmount());
            paymentDto.setCurrency(event.getCurrency());
            paymentDto.setDescription("Pago de reclamación - " + event.getClaimNumber());
            paymentDto.setPaymentType("CLAIM_PAYMENT");

            PaymentDto createdPayment = paymentService.createPayment(paymentDto);

            // Notificar al servicio de reclamaciones que el pago ha sido creado
            try {
                claimServiceClient.updateClaimPaymentStatus(event.getClaimNumber(), "PAYMENT_CREATED");
            } catch (Exception e) {
                log.warn("No se pudo actualizar el estado de la reclamación: {}", e.getMessage());
            }

            log.info("Pago creado para reclamación: {}, ID: {}",
                    event.getClaimNumber(), createdPayment.getId());
        } catch (Exception e) {
            log.error("Error al procesar solicitud de pago para reclamación {}: {}",
                    event.getClaimNumber(), e.getMessage());
        }
    }

    @KafkaListener(topics = "payment.processed", groupId = "claim-service")
    public void consumePaymentProcessedEvent(PaymentProcessedEvent event) {
        log.info("Recibido evento de pago procesado: {}", event.getPaymentNumber());

        // Si el pago está asociado a una reclamación, actualizar el estado
        if (event.getClaimNumber() != null && !event.getClaimNumber().isEmpty()) {
            try {
                claimServiceClient.updateClaimPaymentStatus(event.getClaimNumber(), "PAYMENT_COMPLETED");
                log.info("Estado de reclamación {} actualizado a PAYMENT_COMPLETED", event.getClaimNumber());
            } catch (Exception e) {
                log.error("Error al actualizar estado de la reclamación {}: {}",
                        event.getClaimNumber(), e.getMessage());
            }
        }
    }

    // Clases internas para representar los eventos (en producción estarían en módulos compartidos)

    public static class ClaimPaymentRequestedEvent {
        private String claimNumber;
        private String policyNumber;
        private String customerNumber;
        private BigDecimal amount;
        private String currency;

        // Getters y setters
        public String getClaimNumber() { return claimNumber; }
        public void setClaimNumber(String claimNumber) { this.claimNumber = claimNumber; }

        public String getPolicyNumber() { return policyNumber; }
        public void setPolicyNumber(String policyNumber) { this.policyNumber = policyNumber; }

        public String getCustomerNumber() { return customerNumber; }
        public void setCustomerNumber(String customerNumber) { this.customerNumber = customerNumber; }

        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }

        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
    }

    public static class PaymentProcessedEvent {
        private String paymentNumber;
        private String claimNumber;

        // Getters y setters
        public String getPaymentNumber() { return paymentNumber; }
        public void setPaymentNumber(String paymentNumber) { this.paymentNumber = paymentNumber; }

        public String getClaimNumber() { return claimNumber; }
        public void setClaimNumber(String claimNumber) { this.claimNumber = claimNumber; }
    }
}
package com.insurtech.payment.event.consumer;

import com.insurtech.payment.model.dto.PaymentDto;
import com.insurtech.payment.model.entity.Payment;
import com.insurtech.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
@Slf4j
@RequiredArgsConstructor
public class PolicyPaymentConsumer {

    private final PaymentService paymentService;

    @KafkaListener(topics = "policy.premium.due", groupId = "payment-service")
    public void consumePremiumDueEvent(PolicyPremiumDueEvent event) {
        log.info("Recibido evento de prima pendiente para póliza: {}", event.getPolicyNumber());

        try {
            // Crear un pago pendiente automáticamente para la prima
            PaymentDto paymentDto = new PaymentDto();
            paymentDto.setPolicyNumber(event.getPolicyNumber());
            paymentDto.setCustomerNumber(event.getCustomerNumber());
            paymentDto.setAmount(event.getAmount());
            paymentDto.setCurrency(event.getCurrency());
            paymentDto.setDescription("Prima de seguro - " + event.getPolicyNumber());
            paymentDto.setPaymentType(Payment.PaymentType.PREMIUM);
            paymentDto.setDueDate(event.getDueDate());

            PaymentDto createdPayment = paymentService.createPayment(paymentDto);

            log.info("Pago pendiente creado para prima de póliza: {}, ID: {}",
                    event.getPolicyNumber(), createdPayment.getId());
        } catch (Exception e) {
            log.error("Error al procesar evento de prima pendiente para póliza {}: {}",
                    event.getPolicyNumber(), e.getMessage());
        }
    }

    @KafkaListener(topics = "policy.created", groupId = "payment-service")
    public void consumePolicyCreatedEvent(PolicyCreatedEvent event) {
        log.info("Recibido evento de póliza creada: {}", event.getPolicyNumber());

        // Lógica para gestionar pagos iniciales si es necesario
    }

    @KafkaListener(topics = "policy.renewed", groupId = "payment-service")
    public void consumePolicyRenewedEvent(PolicyRenewedEvent event) {
        log.info("Recibido evento de póliza renovada: {}", event.getPolicyNumber());

        // Lógica para gestionar pagos de renovación
    }

    @KafkaListener(topics = "policy.cancelled", groupId = "payment-service")
    public void consumePolicyCanceledEvent(PolicyCanceledEvent event) {
        log.info("Recibido evento de póliza cancelada: {}", event.getPolicyNumber());

        // Lógica para gestionar reembolsos por cancelación si es necesario
    }

    // Clases internas para representar los eventos (en producción estarían en módulos compartidos)

    public static class PolicyPremiumDueEvent {
        private String policyNumber;
        private String customerNumber;
        private BigDecimal amount;
        private String currency;
        private LocalDateTime dueDate;

        // Getters y setters
        public String getPolicyNumber() { return policyNumber; }
        public void setPolicyNumber(String policyNumber) { this.policyNumber = policyNumber; }

        public String getCustomerNumber() { return customerNumber; }
        public void setCustomerNumber(String customerNumber) { this.customerNumber = customerNumber; }

        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }

        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }

        public LocalDateTime getDueDate() { return dueDate; }
        public void setDueDate(LocalDateTime dueDate) { this.dueDate = dueDate; }
    }

    public static class PolicyCreatedEvent {
        private String policyNumber;
        // Otros campos relevantes

        public String getPolicyNumber() { return policyNumber; }
        public void setPolicyNumber(String policyNumber) { this.policyNumber = policyNumber; }
    }

    public static class PolicyRenewedEvent {
        private String policyNumber;
        // Otros campos relevantes

        public String getPolicyNumber() { return policyNumber; }
        public void setPolicyNumber(String policyNumber) { this.policyNumber = policyNumber; }
    }

    public static class PolicyCanceledEvent {
        private String policyNumber;
        // Otros campos relevantes

        public String getPolicyNumber() { return policyNumber; }
        public void setPolicyNumber(String policyNumber) { this.policyNumber = policyNumber; }
    }
}
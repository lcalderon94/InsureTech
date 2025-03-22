package com.insurtech.payment.model.event;

import com.insurtech.payment.model.entity.Payment;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Evento emitido cuando se crea un nuevo pago
 */
// Update PaymentCreatedEvent.java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCreatedEvent {
    private String eventId;
    private LocalDateTime timestamp;
    private Long paymentId; // Added missing field
    private String paymentNumber;
    private String customerNumber;
    private String policyNumber;
    private String claimNumber; // Added missing field
    private Payment.PaymentType paymentType;
    private BigDecimal amount;
    private String currency;
    private Payment.PaymentStatus status;
    private String paymentMethodNumber;
    private LocalDateTime createdAt;
    private String createdBy;
}
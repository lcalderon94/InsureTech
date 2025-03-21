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
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCreatedEvent {
    private String eventId;
    private String paymentNumber;
    private String customerNumber;
    private String policyNumber;
    private Payment.PaymentType paymentType;
    private BigDecimal amount;
    private String currency;
    private Payment.PaymentStatus status;
    private String paymentMethodNumber;
    private LocalDateTime createdAt;
    private String createdBy;
}
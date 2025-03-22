package com.insurtech.payment.model.event; /**
 * Evento emitido cuando un pago es procesado (exitosa o fallidamente)
 */

import com.insurtech.payment.model.entity.Payment;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentProcessedEvent {
    private String eventId;
    private LocalDateTime timestamp;
    private Long paymentId; // Asegúrese de tener este campo
    private String paymentNumber;
    private String transactionId;
    private String customerNumber;
    private String policyNumber;
    private String claimNumber;
    private BigDecimal amount;
    private String currency;
    private boolean successful;
    private Payment.PaymentStatus status;
    private String gatewayReference;
    private String authorizationCode;
    private String errorCode;
    private String errorMessage;
    private LocalDateTime processedAt;
    private LocalDateTime completionDate;
}
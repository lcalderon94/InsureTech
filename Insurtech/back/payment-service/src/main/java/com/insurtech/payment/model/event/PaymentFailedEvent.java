package com.insurtech.payment.model.event; /**
 * Evento emitido cuando un pago falla
 */
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentFailedEvent {
    private String eventId;
    private String paymentNumber;
    private String transactionId;
    private String customerNumber;
    private String policyNumber;
    private BigDecimal amount;
    private String currency;
    private String failureReason;
    private String errorCode;
    private String errorMessage;
    private boolean retryable;
    private Integer retryCount;
    private LocalDateTime retryScheduledAt;
    private LocalDateTime failedAt;
}
// Cree o actualice la clase RefundProcessedEvent.java
package com.insurtech.payment.model.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefundProcessedEvent {
    private String eventId;
    private LocalDateTime timestamp;
    private Long refundId;
    private String refundNumber;
    private String claimNumber; // Asegúrese de tener este campo
    private String originalPaymentId; // Asegúrese de tener este campo
    private BigDecimal amount;
    private String policyNumber;
    private String customerNumber;
    private LocalDateTime completionDate; // Asegúrese de tener este campo

}
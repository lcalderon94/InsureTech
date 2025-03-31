package com.insurtech.notification.event.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEvent extends BaseEvent {
    private UUID paymentId;
    private String paymentReference;
    private UUID policyId;
    private String policyNumber;
    private UUID customerId;
    private String customerEmail;
    private String customerPhone;
    private String customerName;
    private String paymentStatus; // SUCCESSFUL, FAILED, PENDING
    private String paymentMethod;
    private BigDecimal amount;
    private String currency;
    private LocalDate dueDate;
    private LocalDate paymentDate;
    private String description;
    private Map<String, Object> additionalDetails;
}
package com.insurtech.notification.event.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PolicyEvent extends BaseEvent {
    private UUID policyId;
    private String policyNumber;
    private String policyStatus;
    private String policyType;
    private UUID customerId;
    private String customerEmail;
    private String customerPhone;
    private String customerName;
    private LocalDate effectiveDate;
    private LocalDate expirationDate;
    private String actionType; // CREATED, RENEWED, CANCELLED, UPDATED
    private Map<String, Object> additionalDetails;
}
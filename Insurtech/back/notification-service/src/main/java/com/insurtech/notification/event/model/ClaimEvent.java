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
public class ClaimEvent extends BaseEvent {
    private UUID claimId;
    private String claimNumber;
    private UUID policyId;
    private String policyNumber;
    private UUID customerId;
    private String customerEmail;
    private String customerPhone;
    private String customerName;
    private String claimStatus; // SUBMITTED, IN_REVIEW, APPROVED, REJECTED, PENDING_INFO
    private String previousStatus;
    private String claimType;
    private LocalDate incidentDate;
    private LocalDate reportDate;
    private BigDecimal claimAmount;
    private BigDecimal approvedAmount;
    private String description;
    private String statusNotes;
    private Map<String, Object> additionalDetails;
}
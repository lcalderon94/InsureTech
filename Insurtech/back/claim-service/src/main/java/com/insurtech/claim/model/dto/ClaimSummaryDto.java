package com.insurtech.claim.model.dto;

import com.insurtech.claim.model.entity.Claim;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClaimSummaryDto {

    private Long id;
    private String claimNumber;
    private String policyNumber;
    private String customerNumber;
    private String customerName;
    private LocalDate incidentDate;
    private Claim.ClaimStatus status;
    private Claim.ClaimType claimType;
    private BigDecimal estimatedAmount;
    private BigDecimal approvedAmount;
    private LocalDateTime submissionDate;
    private Long daysOpen;
}
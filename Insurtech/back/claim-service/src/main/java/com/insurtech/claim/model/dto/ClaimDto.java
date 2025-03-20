package com.insurtech.claim.model.dto;

import com.insurtech.claim.model.entity.Claim;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClaimDto {

    private Long id;

    private String claimNumber;

    private Long policyId;

    private String policyNumber;

    private Long customerId;

    private String customerNumber;

    @NotNull(message = "La fecha del incidente es obligatoria")
    private LocalDate incidentDate;

    @NotBlank(message = "La descripción del incidente es obligatoria")
    private String incidentDescription;

    private Claim.ClaimStatus status;

    private Claim.ClaimType claimType;

    private BigDecimal estimatedAmount;

    private BigDecimal approvedAmount;

    private BigDecimal paidAmount;

    private String denialReason;

    private String handlerComments;

    private String customerContactInfo;

    @Valid
    private Set<ClaimItemDto> items = new HashSet<>();

    @Valid
    private Set<ClaimDocumentDto> documents = new HashSet<>();

    private LocalDateTime submissionDate;

    private LocalDateTime assessmentDate;

    private LocalDateTime approvalDate;

    private LocalDateTime settlementDate;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // Campos adicionales para búsqueda
    private String customerEmail;
    private String identificationNumber;
    private String identificationType;
}
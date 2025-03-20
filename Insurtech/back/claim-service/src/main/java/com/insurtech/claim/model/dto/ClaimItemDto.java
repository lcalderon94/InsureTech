package com.insurtech.claim.model.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClaimItemDto {

    private Long id;

    private Long claimId;

    @NotBlank(message = "La descripción del ítem es obligatoria")
    private String description;

    private String category;

    @DecimalMin(value = "0.0", inclusive = false, message = "El valor reclamado debe ser mayor que cero")
    private BigDecimal claimedAmount;

    private BigDecimal approvedAmount;

    private String rejectionReason;

    private boolean covered;

    private String evidenceDocumentId;

    private String additionalDetails;
}
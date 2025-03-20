package com.insurtech.claim.model.dto;

import com.insurtech.claim.model.entity.Claim;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClaimStatusUpdateDto {

    @NotNull(message = "El estado es obligatorio")
    private Claim.ClaimStatus status;

    private String comments;

    private BigDecimal approvedAmount;

    private String denialReason;
}
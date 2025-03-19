package com.insurtech.quote.model.dto;

import com.insurtech.quote.model.entity.QuoteCoverage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuoteCoverageDto {

    private String coverageId;

    @NotBlank(message = "El código de cobertura es obligatorio")
    private String coverageCode;

    @NotBlank(message = "El nombre de cobertura es obligatorio")
    private String coverageName;

    @NotNull(message = "La suma asegurada es obligatoria")
    @DecimalMin(value = "0.0", inclusive = true, message = "La suma asegurada no puede ser negativa")
    private BigDecimal sumInsured;

    private BigDecimal premium;

    private BigDecimal rate;

    private BigDecimal deductible;

    private QuoteCoverage.DeductibleType deductibleType;

    private boolean mandatory;

    private String additionalData;
}
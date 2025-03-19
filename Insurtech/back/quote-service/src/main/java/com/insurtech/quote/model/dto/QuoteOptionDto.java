package com.insurtech.quote.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuoteOptionDto {

    private String optionId;

    @NotBlank(message = "El nombre de la opción es obligatorio")
    private String name;

    private String description;

    @DecimalMin(value = "0.0", inclusive = true, message = "La prima no puede ser negativa")
    private BigDecimal premium;

    @DecimalMin(value = "0.0", inclusive = true, message = "La suma asegurada no puede ser negativa")
    private BigDecimal sumInsured;

    @Valid
    private Set<QuoteCoverageDto> coverages = new HashSet<>();

    private String additionalData;

    private int displayOrder;

    private boolean recommended;
}
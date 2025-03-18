package com.insurtech.policy.model.dto;

import com.insurtech.policy.model.entity.Coverage;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CoverageDto {

    private Long id;

    @NotBlank(message = "El código es obligatorio")
    private String code;

    @NotBlank(message = "El nombre es obligatorio")
    private String name;

    private String description;

    @NotNull(message = "El tipo de cobertura es obligatorio")
    private Coverage.CoverageType coverageType;

    @DecimalMin(value = "0.0", inclusive = true, message = "La tasa de prima predeterminada debe ser mayor o igual a cero")
    private BigDecimal defaultPremiumRate;

    @DecimalMin(value = "0.0", inclusive = true, message = "La suma asegurada predeterminada debe ser mayor o igual a cero")
    private BigDecimal defaultSumInsured;

    private BigDecimal minimumSumInsured;

    private BigDecimal maximumSumInsured;

    private boolean isActive = true;

    private String policyTypes;
}
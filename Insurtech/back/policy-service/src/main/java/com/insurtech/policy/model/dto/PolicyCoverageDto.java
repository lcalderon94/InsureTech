package com.insurtech.policy.model.dto;

import com.insurtech.policy.model.entity.PolicyCoverage;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PolicyCoverageDto {

    private Long id;

    private Long policyId;

    @NotNull(message = "El ID de cobertura es obligatorio")
    private Long coverageId;

    private String coverageCode;

    private String coverageName;

    @DecimalMin(value = "0.0", inclusive = true, message = "La suma asegurada debe ser mayor o igual a cero")
    private BigDecimal sumInsured;

    private BigDecimal premium;

    private BigDecimal premiumRate;

    private BigDecimal deductible;

    private PolicyCoverage.DeductibleType deductibleType;

    private boolean isMandatory;

    private String additionalData;
}
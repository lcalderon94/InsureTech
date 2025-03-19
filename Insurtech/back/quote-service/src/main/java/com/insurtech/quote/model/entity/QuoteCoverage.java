package com.insurtech.quote.model.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuoteCoverage {

    private String coverageId;

    private String coverageCode;

    private String coverageName;

    private BigDecimal sumInsured;

    private BigDecimal premium;

    private BigDecimal rate;

    private BigDecimal deductible;

    private DeductibleType deductibleType;

    private boolean mandatory;

    private String additionalData;

    public enum DeductibleType {
        FIXED, PERCENTAGE
    }
}
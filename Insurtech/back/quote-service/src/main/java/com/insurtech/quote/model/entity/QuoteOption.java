package com.insurtech.quote.model.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuoteOption {

    private String optionId;

    private String name;

    private String description;

    private BigDecimal premium;

    private BigDecimal sumInsured;

    private Set<QuoteCoverage> coverages = new HashSet<>();

    private String additionalData;

    private int displayOrder;

    private boolean recommended;
}
package com.insurtech.quote.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuoteComparisonDto {

    private List<String> quoteNumbers;

    private String customerName;

    private Long customerId;

    private LocalDateTime comparisonDate;

    private Map<String, QuoteOptionDto> recommendedOptions = new HashMap<>();

    private Map<String, Map<String, Boolean>> coverageComparison = new HashMap<>();

    private Map<String, BigDecimal> premiumComparison = new HashMap<>();

    private Map<String, BigDecimal> sumInsuredComparison = new HashMap<>();

    private Map<String, Map<String, Object>> additionalComparisons = new HashMap<>();
}
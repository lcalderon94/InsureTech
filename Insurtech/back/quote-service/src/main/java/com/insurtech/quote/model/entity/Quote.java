package com.insurtech.quote.model.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Document(collection = "quotes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Quote {

    @Id
    private String id;

    private String quoteNumber;

    private Long customerId;

    private String customerEmail;
    private String customerIdentificationNumber;
    private String customerIdentificationType;

    private String customerNumber;

    private QuoteType quoteType;

    private QuoteStatus status;

    private BigDecimal premium;

    private BigDecimal sumInsured;

    private String riskDetails; // JSON con detalles del riesgo según tipo de seguro

    private LocalDateTime effectiveFrom;

    private LocalDateTime effectiveTo;

    private PaymentFrequency paymentFrequency;

    private String additionalInformation;

    private Set<QuoteCoverage> coverages = new HashSet<>();

    private Set<QuoteOption> options = new HashSet<>();

    private LocalDateTime validUntil;

    private LocalDateTime createdAt;

    private String createdBy;

    private LocalDateTime updatedAt;

    private String updatedBy;

    public enum QuoteType {
        AUTO, HOME, LIFE, HEALTH, TRAVEL, BUSINESS, LIABILITY, OTHER
    }

    public enum QuoteStatus {
        DRAFT, COMPLETED, EXPIRED, ACCEPTED, REJECTED, CONVERTED_TO_POLICY
    }

    public enum PaymentFrequency {
        MONTHLY, QUARTERLY, SEMI_ANNUAL, ANNUAL, ONE_TIME
    }
}
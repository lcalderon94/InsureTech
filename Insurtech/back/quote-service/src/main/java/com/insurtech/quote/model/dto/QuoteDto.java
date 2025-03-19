package com.insurtech.quote.model.dto;

import com.insurtech.quote.model.entity.Quote;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuoteDto {

    private String id;

    private String quoteNumber;

    private Long customerId;

    private String customerNumber;

    private String customerEmail;

    private String identificationNumber;

    private String identificationType;

    @NotNull(message = "El tipo de cotización es obligatorio")
    private Quote.QuoteType quoteType;

    private Quote.QuoteStatus status;

    @DecimalMin(value = "0.0", inclusive = true, message = "La prima no puede ser negativa")
    private BigDecimal premium;

    @DecimalMin(value = "0.0", inclusive = true, message = "La suma asegurada no puede ser negativa")
    private BigDecimal sumInsured;

    private String riskDetails; // JSON con detalles del riesgo según tipo de seguro

    @NotNull(message = "La fecha de inicio de vigencia es obligatoria")
    private LocalDateTime effectiveFrom;

    @NotNull(message = "La fecha de fin de vigencia es obligatoria")
    @Future(message = "La fecha de fin de vigencia debe ser futura")
    private LocalDateTime effectiveTo;

    private Quote.PaymentFrequency paymentFrequency;

    private String additionalInformation;

    @Valid
    private Set<QuoteCoverageDto> coverages = new HashSet<>();

    @Valid
    private Set<QuoteOptionDto> options = new HashSet<>();

    private LocalDateTime validUntil;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
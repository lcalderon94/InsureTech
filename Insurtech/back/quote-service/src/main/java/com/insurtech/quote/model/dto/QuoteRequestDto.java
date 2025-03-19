package com.insurtech.quote.model.dto;

import com.insurtech.quote.model.entity.Quote;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuoteRequestDto {

    private Long customerId;

    private String customerNumber;

    private String customerEmail;

    private String identificationNumber;

    private String identificationType;

    @NotNull(message = "El tipo de cotización es obligatorio")
    private Quote.QuoteType quoteType;

    @DecimalMin(value = "0.0", inclusive = true, message = "La suma asegurada no puede ser negativa")
    private BigDecimal sumInsured;

    private Map<String, Object> riskDetails; // Detalles del riesgo según tipo de seguro

    @NotNull(message = "La fecha de inicio de vigencia es obligatoria")
    private LocalDateTime effectiveFrom;

    @NotNull(message = "La fecha de fin de vigencia es obligatoria")
    private LocalDateTime effectiveTo;

    private Quote.PaymentFrequency paymentFrequency;

    private Map<String, Object> additionalInformation;
}
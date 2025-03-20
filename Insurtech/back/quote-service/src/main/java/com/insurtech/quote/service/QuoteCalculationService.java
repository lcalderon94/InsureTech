package com.insurtech.quote.service;

import com.insurtech.quote.model.dto.QuoteCoverageDto;
import com.insurtech.quote.model.dto.QuoteDto;
import com.insurtech.quote.model.dto.QuoteOptionDto;
import com.insurtech.quote.model.dto.QuoteRequestDto;
import com.insurtech.quote.model.entity.Quote;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Set;

public interface QuoteCalculationService {

    /**
     * Calcula la prima para una cotización completa
     */
    Mono<BigDecimal> calculateQuotePremium(QuoteDto quoteDto);

    /**
     * Calcula la prima para una solicitud de cotización
     */
    Mono<BigDecimal> calculateRequestPremium(QuoteRequestDto requestDto);

    /**
     * Calcula la prima para una cobertura específica
     */
    Mono<BigDecimal> calculateCoveragePremium(QuoteCoverageDto coverageDto, Quote.QuoteType quoteType);

    /**
     * Genera opciones de cobertura basadas en la solicitud
     */
    Mono<Set<QuoteOptionDto>> generateQuoteOptions(QuoteRequestDto requestDto);

    /**
     * Aplica descuentos basados en el perfil del cliente y la solicitud
     */
    Mono<BigDecimal> applyDiscounts(BigDecimal basePremium, String customerEmail, QuoteRequestDto requestDto);
}
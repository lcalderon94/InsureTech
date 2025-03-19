package com.insurtech.quote.service;

import com.insurtech.quote.model.dto.*;
import com.insurtech.quote.model.entity.Quote;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface QuoteService {

    /**
     * Crea una nueva cotización basada en la solicitud
     */
    Mono<QuoteResponseDto> createQuote(QuoteRequestDto quoteRequestDto);

    /**
     * Obtiene una cotización por su ID
     */
    Mono<QuoteDto> getQuoteById(String id);

    /**
     * Obtiene una cotización por su número
     */
    Mono<QuoteDto> getQuoteByNumber(String quoteNumber);

    /**
     * Obtiene todas las cotizaciones de un cliente
     */
    Flux<QuoteDto> getQuotesByCustomerId(Long customerId);

    /**
     * Obtiene cotizaciones de un cliente por tipo
     */
    Flux<QuoteDto> getQuotesByCustomerIdAndType(Long customerId, Quote.QuoteType quoteType);

    /**
     * Obtiene todas las cotizaciones activas (no expiradas) de un cliente
     */
    Flux<QuoteDto> getActiveQuotesByCustomerId(Long customerId);

    /**
     * Actualiza una cotización existente
     */
    Mono<QuoteDto> updateQuote(String id, QuoteDto quoteDto);

    /**
     * Actualiza el estado de una cotización
     */
    Mono<QuoteDto> updateQuoteStatus(String id, Quote.QuoteStatus status);

    /**
     * Marca una cotización como aceptada, opcionalmente con una opción seleccionada
     */
    Mono<QuoteDto> acceptQuote(String id, String optionId);

    /**
     * Añade una opción a una cotización
     */
    Mono<QuoteOptionDto> addQuoteOption(String quoteId, QuoteOptionDto optionDto);

    /**
     * Elimina una cotización (sólo en estado DRAFT)
     */
    Mono<Void> deleteQuote(String id);

    /**
     * Genera un número de cotización único
     */
    String generateQuoteNumber(Quote.QuoteType quoteType);

    /**
     * Compara múltiples cotizaciones
     */
    Mono<QuoteComparisonDto> compareQuotes(List<String> quoteNumbers);

    /**
     * Convierte una cotización en póliza (integración con Policy Service)
     */
    Mono<String> convertToPolicyAsync(String quoteId, String selectedOptionId);
}
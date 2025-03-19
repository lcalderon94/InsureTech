package com.insurtech.quote.controller;

import com.insurtech.quote.model.dto.QuoteRequestDto;
import com.insurtech.quote.model.dto.QuoteResponseDto;
import com.insurtech.quote.service.QuoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * Controlador para endpoints públicos relacionados con cotizaciones
 */
@RestController
@RequestMapping("/api/quotes/public")
@Tag(name = "Cotizaciones Públicas", description = "API pública para cotizaciones sin autenticación")
@RequiredArgsConstructor
public class QuotePublicController {

    private static final Logger log = LoggerFactory.getLogger(QuotePublicController.class);

    private final QuoteService quoteService;

    @PostMapping("/quick-quote")
    @Operation(summary = "Solicitar cotización rápida", description = "Genera una cotización rápida sin requerir autenticación")
    public Mono<ResponseEntity<QuoteResponseDto>> createQuickQuote(@Valid @RequestBody QuoteRequestDto quoteRequestDto) {
        log.info("Solicitud recibida para cotización rápida de tipo: {}", quoteRequestDto.getQuoteType());
        return quoteService.createQuote(quoteRequestDto)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
    }

    @GetMapping("/verify/{quoteNumber}")
    @Operation(summary = "Verificar cotización", description = "Verifica si una cotización existe y está vigente")
    public Mono<ResponseEntity<Boolean>> verifyQuote(@PathVariable String quoteNumber) {
        log.info("Verificando cotización con número: {}", quoteNumber);
        return quoteService.getQuoteByNumber(quoteNumber)
                .map(quote -> {
                    boolean isValid = quote.getValidUntil() != null &&
                            quote.getValidUntil().isAfter(java.time.LocalDateTime.now());
                    return ResponseEntity.ok(isValid);
                })
                .defaultIfEmpty(ResponseEntity.ok(false));
    }
}
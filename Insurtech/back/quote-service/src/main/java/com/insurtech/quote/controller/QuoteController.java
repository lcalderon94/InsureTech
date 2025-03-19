package com.insurtech.quote.controller;

import com.insurtech.quote.model.dto.*;
import com.insurtech.quote.model.entity.Quote;
import com.insurtech.quote.service.QuoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/quotes")
@Tag(name = "Cotizaciones", description = "API para la gestión de cotizaciones de seguros")
@SecurityRequirement(name = "bearer-jwt")
@RequiredArgsConstructor
public class QuoteController {

    private static final Logger log = LoggerFactory.getLogger(QuoteController.class);

    private final QuoteService quoteService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Crear una nueva cotización", description = "Genera una nueva cotización con múltiples opciones")
    public Mono<ResponseEntity<QuoteResponseDto>> createQuote(@Valid @RequestBody QuoteRequestDto quoteRequestDto) {
        log.info("Solicitud recibida para crear cotización de tipo: {}", quoteRequestDto.getQuoteType());
        return quoteService.createQuote(quoteRequestDto)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT') or hasRole('USER')")
    @Operation(summary = "Obtener cotización por ID", description = "Obtiene una cotización por su ID")
    public Mono<ResponseEntity<QuoteDto>> getQuoteById(@PathVariable String id) {
        log.info("Obteniendo cotización por ID: {}", id);
        return quoteService.getQuoteById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/number/{quoteNumber}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT') or hasRole('USER')")
    @Operation(summary = "Obtener cotización por número", description = "Obtiene una cotización por su número")
    public Mono<ResponseEntity<QuoteDto>> getQuoteByNumber(@PathVariable String quoteNumber) {
        log.info("Obteniendo cotización por número: {}", quoteNumber);
        return quoteService.getQuoteByNumber(quoteNumber)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT') or @securityService.isCustomerOwner(#customerId)")
    @Operation(summary = "Obtener cotizaciones por cliente", description = "Obtiene todas las cotizaciones de un cliente")
    public Flux<QuoteDto> getQuotesByCustomerId(@PathVariable Long customerId) {
        log.info("Obteniendo cotizaciones para cliente ID: {}", customerId);
        return quoteService.getQuotesByCustomerId(customerId);
    }

    @GetMapping("/customer/{customerId}/active")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT') or @securityService.isCustomerOwner(#customerId)")
    @Operation(summary = "Obtener cotizaciones activas por cliente", description = "Obtiene todas las cotizaciones activas de un cliente")
    public Flux<QuoteDto> getActiveQuotesByCustomerId(@PathVariable Long customerId) {
        log.info("Obteniendo cotizaciones activas para cliente ID: {}", customerId);
        return quoteService.getActiveQuotesByCustomerId(customerId);
    }

    @GetMapping("/customer/{customerId}/type/{quoteType}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT') or @securityService.isCustomerOwner(#customerId)")
    @Operation(summary = "Obtener cotizaciones por cliente y tipo", description = "Obtiene todas las cotizaciones de un cliente para un tipo específico")
    public Flux<QuoteDto> getQuotesByCustomerIdAndType(
            @PathVariable Long customerId,
            @PathVariable Quote.QuoteType quoteType) {
        log.info("Obteniendo cotizaciones de tipo {} para cliente ID: {}", quoteType, customerId);
        return quoteService.getQuotesByCustomerIdAndType(customerId, quoteType);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Actualizar cotización", description = "Actualiza una cotización existente")
    public Mono<ResponseEntity<QuoteDto>> updateQuote(
            @PathVariable String id,
            @Valid @RequestBody QuoteDto quoteDto) {
        log.info("Actualizando cotización con ID: {}", id);
        return quoteService.updateQuote(id, quoteDto)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Actualizar estado de cotización", description = "Actualiza el estado de una cotización")
    public Mono<ResponseEntity<QuoteDto>> updateQuoteStatus(
            @PathVariable String id,
            @RequestParam Quote.QuoteStatus status) {
        log.info("Actualizando estado a {} para cotización ID: {}", status, id);
        return quoteService.updateQuoteStatus(id, status)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/accept")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT') or hasRole('USER')")
    @Operation(summary = "Aceptar cotización", description = "Marca una cotización como aceptada y selecciona una opción")
    public Mono<ResponseEntity<QuoteDto>> acceptQuote(
            @PathVariable String id,
            @RequestParam(required = false) String optionId) {
        log.info("Aceptando cotización ID: {} con opción ID: {}", id, optionId);
        return quoteService.acceptQuote(id, optionId)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PostMapping("/{quoteId}/options")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Añadir opción a cotización", description = "Añade una nueva opción a una cotización existente")
    public Mono<ResponseEntity<QuoteOptionDto>> addQuoteOption(
            @PathVariable String quoteId,
            @Valid @RequestBody QuoteOptionDto optionDto) {
        log.info("Añadiendo opción a cotización ID: {}", quoteId);
        return quoteService.addQuoteOption(quoteId, optionDto)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Eliminar cotización", description = "Elimina una cotización (solo en estado DRAFT)")
    public Mono<ResponseEntity<Void>> deleteQuote(@PathVariable String id) {
        log.info("Eliminando cotización con ID: {}", id);
        return quoteService.deleteQuote(id)
                .then(Mono.just(ResponseEntity.noContent().<Void>build()))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PostMapping("/compare")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT') or hasRole('USER')")
    @Operation(summary = "Comparar cotizaciones", description = "Compara múltiples cotizaciones")
    public Mono<ResponseEntity<QuoteComparisonDto>> compareQuotes(
            @RequestBody List<String> quoteNumbers) {
        log.info("Comparando cotizaciones: {}", quoteNumbers);
        return quoteService.compareQuotes(quoteNumbers)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.badRequest().build());
    }

    @PostMapping("/{quoteId}/convert-to-policy")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Convertir a póliza", description = "Convierte una cotización en póliza")
    public Mono<ResponseEntity<String>> convertToPolicyAsync(
            @PathVariable String quoteId,
            @RequestParam(required = false) String selectedOptionId) {
        log.info("Convirtiendo cotización ID: {} a póliza con opción ID: {}", quoteId, selectedOptionId);
        return quoteService.convertToPolicyAsync(quoteId, selectedOptionId)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.badRequest().build());
    }
}
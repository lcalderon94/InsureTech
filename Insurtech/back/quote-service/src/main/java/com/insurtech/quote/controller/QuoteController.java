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

    // Reemplazar el endpoint basado en ID con número de cotización
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

    // Modificar para usar número de cotización
    @PutMapping("/number/{quoteNumber}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Actualizar cotización", description = "Actualiza una cotización existente")
    public Mono<ResponseEntity<QuoteDto>> updateQuote(
            @PathVariable String quoteNumber,
            @Valid @RequestBody QuoteDto quoteDto) {
        log.info("Actualizando cotización con número: {}", quoteNumber);
        return quoteService.getQuoteByNumber(quoteNumber)
                .flatMap(existingQuote -> quoteService.updateQuote(existingQuote.getId(), quoteDto))
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    // Modificar para usar número de cotización
    @PatchMapping("/number/{quoteNumber}/status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Actualizar estado de cotización", description = "Actualiza el estado de una cotización")
    public Mono<ResponseEntity<QuoteDto>> updateQuoteStatus(
            @PathVariable String quoteNumber,
            @RequestParam Quote.QuoteStatus status) {
        log.info("Actualizando estado a {} para cotización número: {}", status, quoteNumber);
        return quoteService.getQuoteByNumber(quoteNumber)
                .flatMap(existingQuote -> quoteService.updateQuoteStatus(existingQuote.getId(), status))
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    // Modificar para usar número de cotización
    @PostMapping("/number/{quoteNumber}/accept")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT') or hasRole('USER')")
    @Operation(summary = "Aceptar cotización", description = "Marca una cotización como aceptada y selecciona una opción")
    public Mono<ResponseEntity<QuoteDto>> acceptQuote(
            @PathVariable String quoteNumber,
            @RequestParam(required = false) String optionId) {
        log.info("Aceptando cotización número: {} con opción ID: {}", quoteNumber, optionId);
        return quoteService.getQuoteByNumber(quoteNumber)
                .flatMap(existingQuote -> quoteService.acceptQuote(existingQuote.getId(), optionId))
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    // Modificar para usar número de cotización
    @PostMapping("/number/{quoteNumber}/options")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Añadir opción a cotización", description = "Añade una nueva opción a una cotización existente")
    public Mono<ResponseEntity<QuoteOptionDto>> addQuoteOption(
            @PathVariable String quoteNumber,
            @Valid @RequestBody QuoteOptionDto optionDto) {
        log.info("Añadiendo opción a cotización número: {}", quoteNumber);
        return quoteService.getQuoteByNumber(quoteNumber)
                .flatMap(existingQuote -> quoteService.addQuoteOption(existingQuote.getId(), optionDto))
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    // Modificar para usar número de cotización
    @DeleteMapping("/number/{quoteNumber}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Eliminar cotización", description = "Elimina una cotización (solo en estado DRAFT)")
    public Mono<ResponseEntity<Void>> deleteQuote(@PathVariable String quoteNumber) {
        log.info("Eliminando cotización con número: {}", quoteNumber);
        return quoteService.getQuoteByNumber(quoteNumber)
                .flatMap(existingQuote -> quoteService.deleteQuote(existingQuote.getId())
                        .then(Mono.just(ResponseEntity.noContent().<Void>build())))
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

    // Modificar para usar número de cotización
    @PostMapping("/number/{quoteNumber}/convert-to-policy")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Convertir a póliza", description = "Convierte una cotización en póliza")
    public Mono<ResponseEntity<String>> convertToPolicyAsync(
            @PathVariable String quoteNumber,
            @RequestParam(required = false) String selectedOptionId) {
        log.info("Convirtiendo cotización número: {} a póliza con opción ID: {}", quoteNumber, selectedOptionId);
        return quoteService.getQuoteByNumber(quoteNumber)
                .flatMap(existingQuote -> quoteService.convertToPolicyAsync(existingQuote.getId(), selectedOptionId))
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.badRequest().build());
    }

    // Mantener endpoint por ID solo para compatibilidad interna y operaciones de sistema
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Obtener cotización por ID (Solo sistema)", description = "Obtiene una cotización por su ID interno (uso restringido)")
    public Mono<ResponseEntity<QuoteDto>> getQuoteById(@PathVariable String id) {
        log.info("Obteniendo cotización por ID interno: {}", id);
        return quoteService.getQuoteById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}
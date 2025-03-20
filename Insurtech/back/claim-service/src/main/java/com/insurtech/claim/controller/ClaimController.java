package com.insurtech.claim.controller;

import com.insurtech.claim.exception.ClaimNotFoundException;
import com.insurtech.claim.model.dto.*;
import com.insurtech.claim.model.entity.Claim;
import com.insurtech.claim.service.ClaimService;
import com.insurtech.claim.service.ClaimDocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/claims")
@Tag(name = "Reclamaciones", description = "API para la gestión de reclamaciones de seguros")
@SecurityRequirement(name = "bearer-jwt")
@RequiredArgsConstructor
public class ClaimController {

    private static final Logger log = LoggerFactory.getLogger(ClaimController.class);

    private final ClaimService claimService;
    private final ClaimDocumentService documentService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT') or hasRole('USER')")
    @Operation(summary = "Crear una nueva reclamación", description = "Crea una nueva reclamación de seguro")
    public ResponseEntity<ClaimDto> createClaim(@Valid @RequestBody ClaimDto claimDto) {
        log.info("Solicitud recibida para crear reclamación");
        ClaimDto createdClaim = claimService.createClaim(claimDto);
        return new ResponseEntity<>(createdClaim, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Obtener reclamación por ID (Solo sistema)", description = "Obtiene una reclamación por su ID interno (uso restringido)")
    public ResponseEntity<ClaimDto> getClaimById(@PathVariable Long id) {
        log.info("Obteniendo reclamación por ID interno: {}", id);
        return claimService.getClaimById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ClaimNotFoundException("Reclamación no encontrada con ID: " + id));
    }

    @GetMapping("/number/{claimNumber}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT') or hasRole('USER')")
    @Operation(summary = "Obtener reclamación por número", description = "Obtiene una reclamación por su número")
    public ResponseEntity<ClaimDto> getClaimByNumber(@PathVariable String claimNumber) {
        log.info("Obteniendo reclamación por número: {}", claimNumber);
        return claimService.getClaimByNumber(claimNumber)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ClaimNotFoundException("Reclamación no encontrada con número: " + claimNumber));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Buscar reclamaciones", description = "Busca reclamaciones por término de búsqueda")
    public ResponseEntity<Page<ClaimDto>> searchClaims(
            @RequestParam(required = false) String searchTerm,
            Pageable pageable) {
        log.info("Buscando reclamaciones con término: {}", searchTerm);
        Page<ClaimDto> claims = claimService.searchClaims(searchTerm, pageable);
        return ResponseEntity.ok(claims);
    }

    @PostMapping("/search")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Búsqueda avanzada de reclamaciones", description = "Realiza una búsqueda avanzada de reclamaciones")
    public ResponseEntity<List<ClaimDto>> advancedSearch(@RequestBody ClaimSearchRequestDto searchRequest) {
        log.info("Realizando búsqueda avanzada de reclamaciones");
        List<ClaimDto> claims = claimService.advancedSearch(searchRequest);
        return ResponseEntity.ok(claims);
    }

    @GetMapping("/customer/{customerNumber}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT') or hasRole('USER')")
    @Operation(summary = "Obtener reclamaciones por cliente", description = "Obtiene todas las reclamaciones de un cliente")
    public ResponseEntity<List<ClaimDto>> getClaimsByCustomerNumber(@PathVariable String customerNumber) {
        log.info("Obteniendo reclamaciones para cliente número: {}", customerNumber);
        List<ClaimDto> claims = claimService.getClaimsByCustomerNumber(customerNumber);
        return ResponseEntity.ok(claims);
    }

    @GetMapping("/policy/{policyNumber}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT') or hasRole('USER')")
    @Operation(summary = "Obtener reclamaciones por póliza", description = "Obtiene todas las reclamaciones de una póliza")
    public ResponseEntity<List<ClaimDto>> getClaimsByPolicyNumber(@PathVariable String policyNumber) {
        log.info("Obteniendo reclamaciones para póliza número: {}", policyNumber);
        List<ClaimDto> claims = claimService.getClaimsByPolicyNumber(policyNumber);
        return ResponseEntity.ok(claims);
    }

    @PutMapping("/number/{claimNumber}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Actualizar reclamación", description = "Actualiza una reclamación existente")
    public ResponseEntity<ClaimDto> updateClaim(
            @PathVariable String claimNumber,
            @Valid @RequestBody ClaimDto claimDto) {
        log.info("Actualizando reclamación con número: {}", claimNumber);
        return claimService.getClaimByNumber(claimNumber)
                .map(existingClaim -> {
                    ClaimDto updatedClaim = claimService.updateClaim(existingClaim.getId(), claimDto);
                    return ResponseEntity.ok(updatedClaim);
                })
                .orElseThrow(() -> new ClaimNotFoundException("Reclamación no encontrada con número: " + claimNumber));
    }

    @PatchMapping("/number/{claimNumber}/status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Actualizar estado de reclamación", description = "Actualiza el estado de una reclamación")
    public ResponseEntity<ClaimDto> updateClaimStatus(
            @PathVariable String claimNumber,
            @Valid @RequestBody ClaimStatusUpdateDto statusUpdateDto) {
        log.info("Actualizando estado a {} para reclamación número: {}", statusUpdateDto.getStatus(), claimNumber);
        return claimService.getClaimByNumber(claimNumber)
                .map(existingClaim -> {
                    ClaimDto updatedClaim = claimService.updateClaimStatus(
                            existingClaim.getId(),
                            statusUpdateDto.getStatus(),
                            statusUpdateDto.getComments(),
                            statusUpdateDto.getApprovedAmount(),
                            statusUpdateDto.getDenialReason()
                    );
                    return ResponseEntity.ok(updatedClaim);
                })
                .orElseThrow(() -> new ClaimNotFoundException("Reclamación no encontrada con número: " + claimNumber));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Obtener reclamaciones por estado", description = "Obtiene todas las reclamaciones en un estado específico")
    public ResponseEntity<List<ClaimDto>> getClaimsByStatus(@PathVariable Claim.ClaimStatus status) {
        log.info("Obteniendo reclamaciones con estado: {}", status);
        List<ClaimDto> claims = claimService.getClaimsByStatus(status);
        return ResponseEntity.ok(claims);
    }

    @GetMapping("/incident-date")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Obtener reclamaciones por fecha de incidente", description = "Obtiene reclamaciones que ocurrieron en un rango de fechas")
    public ResponseEntity<List<ClaimDto>> getClaimsByIncidentDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.info("Obteniendo reclamaciones con fecha de incidente entre {} y {}", startDate, endDate);
        List<ClaimDto> claims = claimService.getClaimsByIncidentDateRange(startDate, endDate);
        return ResponseEntity.ok(claims);
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Obtener estadísticas para dashboard", description = "Obtiene estadísticas de reclamaciones para dashboard")
    public ResponseEntity<Map<String, Object>> getClaimsDashboardStatistics() {
        log.info("Obteniendo estadísticas de reclamaciones para dashboard");
        Map<String, Object> statistics = claimService.getClaimsDashboardStatistics();
        return ResponseEntity.ok(statistics);
    }

    @GetMapping("/analytics")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Análisis avanzado de reclamaciones", description = "Realiza un análisis avanzado de reclamaciones con múltiples métricas")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> getClaimsAdvancedAnalytics() {
        log.info("Realizando análisis avanzado de reclamaciones");
        return claimService.getClaimsAdvancedAnalyticsAsync()
                .thenApply(ResponseEntity::ok);
    }

    @PostMapping("/number/{claimNumber}/items")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Añadir ítem a reclamación", description = "Añade un ítem a una reclamación existente")
    public ResponseEntity<ClaimItemDto> addClaimItem(
            @PathVariable String claimNumber,
            @Valid @RequestBody ClaimItemDto itemDto) {
        log.info("Añadiendo ítem a reclamación número: {}", claimNumber);
        return claimService.getClaimByNumber(claimNumber)
                .map(existingClaim -> {
                    ClaimItemDto createdItem = claimService.addClaimItem(existingClaim.getId(), itemDto);
                    return new ResponseEntity<>(createdItem, HttpStatus.CREATED);
                })
                .orElseThrow(() -> new ClaimNotFoundException("Reclamación no encontrada con número: " + claimNumber));
    }

    @GetMapping("/number/{claimNumber}/items")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT') or hasRole('USER')")
    @Operation(summary = "Obtener ítems de reclamación", description = "Obtiene todos los ítems de una reclamación")
    public ResponseEntity<List<ClaimItemDto>> getClaimItems(@PathVariable String claimNumber) {
        log.info("Obteniendo ítems para reclamación número: {}", claimNumber);
        return claimService.getClaimByNumber(claimNumber)
                .map(existingClaim -> {
                    List<ClaimItemDto> items = claimService.getClaimItems(existingClaim.getId());
                    return ResponseEntity.ok(items);
                })
                .orElseThrow(() -> new ClaimNotFoundException("Reclamación no encontrada con número: " + claimNumber));
    }

    @PatchMapping("/number/{claimNumber}/items/{itemId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Actualizar ítem de reclamación", description = "Actualiza un ítem existente de una reclamación")
    public ResponseEntity<ClaimItemDto> updateClaimItem(
            @PathVariable String claimNumber,
            @PathVariable Long itemId,
            @Valid @RequestBody ClaimItemDto itemDto) {
        log.info("Actualizando ítem ID {} de reclamación número: {}", itemId, claimNumber);
        ClaimItemDto updatedItem = claimService.updateClaimItem(claimNumber, itemId, itemDto);
        return ResponseEntity.ok(updatedItem);
    }

    @GetMapping("/number/{claimNumber}/documents")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT') or hasRole('USER')")
    @Operation(summary = "Obtener documentos de reclamación", description = "Obtiene todos los documentos de una reclamación")
    public ResponseEntity<List<ClaimDocumentDto>> getClaimDocuments(@PathVariable String claimNumber) {
        log.info("Obteniendo documentos para reclamación número: {}", claimNumber);
        return claimService.getClaimByNumber(claimNumber)
                .map(existingClaim -> {
                    List<ClaimDocumentDto> documents = documentService.getClaimDocuments(existingClaim.getId());
                    return ResponseEntity.ok(documents);
                })
                .orElseThrow(() -> new ClaimNotFoundException("Reclamación no encontrada con número: " + claimNumber));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Resumen de reclamaciones", description = "Obtiene un resumen de reclamaciones con filtros opcionales")
    public ResponseEntity<List<ClaimSummaryDto>> getClaimsSummary(
            @RequestParam(required = false) Claim.ClaimStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        log.info("Obteniendo resumen de reclamaciones");
        List<ClaimSummaryDto> summary = claimService.getClaimsSummary(status, from, to);
        return ResponseEntity.ok(summary);
    }
}
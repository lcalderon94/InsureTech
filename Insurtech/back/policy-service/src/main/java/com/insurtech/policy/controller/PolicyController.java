package com.insurtech.policy.controller;

import com.insurtech.policy.exception.PolicyNotFoundException;
import com.insurtech.policy.model.dto.PolicyDto;
import com.insurtech.policy.model.dto.PolicyNoteDto;
import com.insurtech.policy.model.entity.Policy;
import com.insurtech.policy.service.PolicyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/policies")
@Tag(name = "Pólizas", description = "API para la gestión de pólizas de seguros")
@SecurityRequirement(name = "bearer-jwt")
@RequiredArgsConstructor
public class PolicyController {

    private static final Logger log = LoggerFactory.getLogger(PolicyController.class);

    private final PolicyService policyService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(
            summary = "Crear una nueva póliza",
            description = "Crea una nueva póliza de seguro. El cliente puede ser identificado mediante customerId, customerEmail, identificationNumber+identificationType, o customerNumber."
    )
    public ResponseEntity<PolicyDto> createPolicy(
            @Valid @RequestBody
            @Schema(description = "Datos de la póliza. Para identificar al cliente puede proporcionar: " +
                    "customerId directo, customerEmail, identificationNumber+identificationType, o customerNumber")
            PolicyDto policyDto) {

        log.info("Solicitud recibida para crear póliza");
        PolicyDto createdPolicy = policyService.createPolicy(policyDto);
        return new ResponseEntity<>(createdPolicy, HttpStatus.CREATED);
    }

    // Mantener endpoint por ID solo para compatibilidad interna y operaciones de sistema
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Obtener póliza por ID (Solo sistema)", description = "Obtiene una póliza por su ID interno (uso restringido)")
    public ResponseEntity<PolicyDto> getPolicyById(@PathVariable Long id) {
        log.info("Obteniendo póliza por ID interno: {}", id);
        return policyService.getPolicyById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new PolicyNotFoundException("Póliza no encontrada con ID: " + id));
    }

    @GetMapping("/number/{policyNumber}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT') or hasRole('USER')")
    @Operation(summary = "Obtener póliza por número", description = "Obtiene una póliza por su número")
    public ResponseEntity<PolicyDto> getPolicyByNumber(@PathVariable String policyNumber) {
        log.info("Obteniendo póliza por número: {}", policyNumber);
        return policyService.getPolicyByNumber(policyNumber)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new PolicyNotFoundException("Póliza no encontrada con número: " + policyNumber));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Buscar pólizas", description = "Busca pólizas por término de búsqueda")
    public ResponseEntity<Page<PolicyDto>> searchPolicies(
            @RequestParam(required = false) String searchTerm,
            Pageable pageable) {
        log.info("Buscando pólizas con término: {}", searchTerm);
        Page<PolicyDto> policies = policyService.searchPolicies(searchTerm, pageable);
        return ResponseEntity.ok(policies);
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT') or @securityService.isCustomerOwner(#customerId)")
    @Operation(summary = "Obtener pólizas por cliente", description = "Obtiene todas las pólizas de un cliente")
    public ResponseEntity<List<PolicyDto>> getPoliciesByCustomerId(@PathVariable Long customerId) {
        log.info("Obteniendo pólizas para cliente ID: {}", customerId);
        List<PolicyDto> policies = policyService.getPoliciesByCustomerId(customerId);
        return ResponseEntity.ok(policies);
    }

    // Modificar para usar número de póliza
    @PutMapping("/number/{policyNumber}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Actualizar póliza", description = "Actualiza una póliza existente")
    public ResponseEntity<PolicyDto> updatePolicy(
            @PathVariable String policyNumber,
            @Valid @RequestBody PolicyDto policyDto) {
        log.info("Actualizando póliza con número: {}", policyNumber);
        return policyService.getPolicyByNumber(policyNumber)
                .map(existingPolicy -> {
                    PolicyDto updatedPolicy = policyService.updatePolicy(existingPolicy.getId(), policyDto);
                    return ResponseEntity.ok(updatedPolicy);
                })
                .orElseThrow(() -> new PolicyNotFoundException("Póliza no encontrada con número: " + policyNumber));
    }

    // Modificar para usar número de póliza
    @DeleteMapping("/number/{policyNumber}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Eliminar póliza", description = "Elimina una póliza (solo si está en estado DRAFT)")
    public ResponseEntity<Void> deletePolicy(@PathVariable String policyNumber) {
        log.info("Eliminando póliza con número: {}", policyNumber);
        return policyService.getPolicyByNumber(policyNumber)
                .map(existingPolicy -> {
                    policyService.deletePolicy(existingPolicy.getId());
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElseThrow(() -> new PolicyNotFoundException("Póliza no encontrada con número: " + policyNumber));
    }

    // Modificar para usar número de póliza
    @PatchMapping("/number/{policyNumber}/status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Actualizar estado de póliza", description = "Actualiza el estado de una póliza")
    public ResponseEntity<PolicyDto> updatePolicyStatus(
            @PathVariable String policyNumber,
            @RequestParam Policy.PolicyStatus status,
            @RequestParam String reason) {
        log.info("Actualizando estado a {} para póliza número: {}", status, policyNumber);
        return policyService.getPolicyByNumber(policyNumber)
                .map(existingPolicy -> {
                    PolicyDto updatedPolicy = policyService.updatePolicyStatus(existingPolicy.getId(), status, reason);
                    return ResponseEntity.ok(updatedPolicy);
                })
                .orElseThrow(() -> new PolicyNotFoundException("Póliza no encontrada con número: " + policyNumber));
    }

    // Modificar para usar número de póliza
    @PostMapping("/number/{policyNumber}/notes")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Añadir nota a póliza", description = "Añade una nota a una póliza")
    public ResponseEntity<PolicyNoteDto> addPolicyNote(
            @PathVariable String policyNumber,
            @Valid @RequestBody PolicyNoteDto noteDto) {
        log.info("Añadiendo nota a póliza número: {}", policyNumber);
        return policyService.getPolicyByNumber(policyNumber)
                .map(existingPolicy -> {
                    PolicyNoteDto createdNote = policyService.addPolicyNote(existingPolicy.getId(), noteDto);
                    return new ResponseEntity<>(createdNote, HttpStatus.CREATED);
                })
                .orElseThrow(() -> new PolicyNotFoundException("Póliza no encontrada con número: " + policyNumber));
    }

    @GetMapping("/expiring")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Obtener pólizas a expirar", description = "Obtiene pólizas que expiran en un rango de fechas")
    public ResponseEntity<List<PolicyDto>> getPoliciesExpiringBetween(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.info("Obteniendo pólizas que expiran entre {} y {}", startDate, endDate);
        List<PolicyDto> policies = policyService.getPoliciesExpiringBetween(startDate, endDate);
        return ResponseEntity.ok(policies);
    }

    @PostMapping("/calculate-premium")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Calcular prima", description = "Calcula la prima para una póliza de forma asíncrona")
    public CompletableFuture<ResponseEntity<Double>> calculatePremiumAsync(@RequestBody PolicyDto policyDto) {
        log.info("Calculando prima de forma asíncrona");
        return policyService.calculatePremiumAsync(policyDto)
                .thenApply(ResponseEntity::ok);
    }

    @GetMapping("/customer/{customerId}/statistics")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT') or @securityService.isCustomerOwner(#customerId)")
    @Operation(summary = "Obtener estadísticas de cliente", description = "Calcula estadísticas de pólizas para un cliente")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> getCustomerPolicyStatistics(
            @PathVariable Long customerId) {
        log.info("Calculando estadísticas para cliente ID: {}", customerId);
        return policyService.calculateCustomerPolicyStatisticsAsync(customerId)
                .thenApply(ResponseEntity::ok);
    }

    @PostMapping("/validate")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Validar póliza", description = "Valida una póliza de forma asíncrona")
    public CompletableFuture<ResponseEntity<Boolean>> validatePolicyAsync(@RequestBody PolicyDto policyDto) {
        log.info("Validando póliza de forma asíncrona");
        return policyService.validatePolicyAsync(policyDto)
                .thenApply(ResponseEntity::ok);
    }
}
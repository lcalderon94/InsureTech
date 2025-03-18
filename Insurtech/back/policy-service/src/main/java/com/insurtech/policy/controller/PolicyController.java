package com.insurtech.policy.controller;

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
    @Operation(summary = "Crear una nueva póliza", description = "Crea una nueva póliza de seguro")
    public ResponseEntity<PolicyDto> createPolicy(@Valid @RequestBody PolicyDto policyDto) {
        log.info("Creando nueva póliza");
        PolicyDto createdPolicy = policyService.createPolicy(policyDto);
        return new ResponseEntity<>(createdPolicy, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT') or hasRole('USER')")
    @Operation(summary = "Obtener póliza por ID", description = "Obtiene una póliza por su ID")
    public ResponseEntity<PolicyDto> getPolicyById(@PathVariable Long id) {
        log.info("Obteniendo póliza por ID: {}", id);
        return policyService.getPolicyById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/number/{policyNumber}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT') or hasRole('USER')")
    @Operation(summary = "Obtener póliza por número", description = "Obtiene una póliza por su número")
    public ResponseEntity<PolicyDto> getPolicyByNumber(@PathVariable String policyNumber) {
        log.info("Obteniendo póliza por número: {}", policyNumber);
        return policyService.getPolicyByNumber(policyNumber)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
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

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Actualizar póliza", description = "Actualiza una póliza existente")
    public ResponseEntity<PolicyDto> updatePolicy(
            @PathVariable Long id,
            @Valid @RequestBody PolicyDto policyDto) {
        log.info("Actualizando póliza con ID: {}", id);
        PolicyDto updatedPolicy = policyService.updatePolicy(id, policyDto);
        return ResponseEntity.ok(updatedPolicy);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Eliminar póliza", description = "Elimina una póliza (solo si está en estado DRAFT)")
    public ResponseEntity<Void> deletePolicy(@PathVariable Long id) {
        log.info("Eliminando póliza con ID: {}", id);
        policyService.deletePolicy(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Actualizar estado de póliza", description = "Actualiza el estado de una póliza")
    public ResponseEntity<PolicyDto> updatePolicyStatus(
            @PathVariable Long id,
            @RequestParam Policy.PolicyStatus status,
            @RequestParam String reason) {
        log.info("Actualizando estado a {} para póliza ID: {}", status, id);
        PolicyDto updatedPolicy = policyService.updatePolicyStatus(id, status, reason);
        return ResponseEntity.ok(updatedPolicy);
    }

    @PostMapping("/{policyId}/notes")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    @Operation(summary = "Añadir nota a póliza", description = "Añade una nota a una póliza")
    public ResponseEntity<PolicyNoteDto> addPolicyNote(
            @PathVariable Long policyId,
            @Valid @RequestBody PolicyNoteDto noteDto) {
        log.info("Añadiendo nota a póliza ID: {}", policyId);
        PolicyNoteDto createdNote = policyService.addPolicyNote(policyId, noteDto);
        return new ResponseEntity<>(createdNote, HttpStatus.CREATED);
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
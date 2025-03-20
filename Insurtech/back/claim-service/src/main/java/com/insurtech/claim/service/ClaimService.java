package com.insurtech.claim.service;

import com.insurtech.claim.model.dto.ClaimDto;
import com.insurtech.claim.model.dto.ClaimItemDto;
import com.insurtech.claim.model.dto.ClaimSearchRequestDto;
import com.insurtech.claim.model.dto.ClaimSummaryDto;
import com.insurtech.claim.model.entity.Claim;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface ClaimService {

    /**
     * Crea una nueva reclamación
     */
    ClaimDto createClaim(ClaimDto claimDto);

    /**
     * Obtiene una reclamación por su ID (método interno)
     */
    Optional<ClaimDto> getClaimById(Long id);

    /**
     * Obtiene una reclamación por su número
     */
    Optional<ClaimDto> getClaimByNumber(String claimNumber);

    /**
     * Busca reclamaciones por término de búsqueda
     */
    Page<ClaimDto> searchClaims(String searchTerm, Pageable pageable);

    /**
     * Realiza una búsqueda avanzada de reclamaciones
     */
    List<ClaimDto> advancedSearch(ClaimSearchRequestDto searchRequest);

    /**
     * Obtiene todas las reclamaciones de un cliente
     */
    List<ClaimDto> getClaimsByCustomerId(Long customerId);

    /**
     * Obtiene todas las reclamaciones de un cliente por número de cliente
     */
    List<ClaimDto> getClaimsByCustomerNumber(String customerNumber);

    /**
     * Obtiene todas las reclamaciones de un cliente por email
     */
    List<ClaimDto> getClaimsByCustomerEmail(String email);

    /**
     * Obtiene todas las reclamaciones de una póliza
     */
    List<ClaimDto> getClaimsByPolicyId(Long policyId);

    /**
     * Obtiene todas las reclamaciones de una póliza por número de póliza
     */
    List<ClaimDto> getClaimsByPolicyNumber(String policyNumber);

    /**
     * Actualiza una reclamación existente por ID (método interno)
     */
    ClaimDto updateClaim(Long id, ClaimDto claimDto);

    /**
     * Actualiza una reclamación existente por número de reclamación
     */
    ClaimDto updateClaimByNumber(String claimNumber, ClaimDto claimDto);

    /**
     * Actualiza el estado de una reclamación por ID (método interno)
     */
    ClaimDto updateClaimStatus(Long id, Claim.ClaimStatus status, String comments, BigDecimal approvedAmount, String denialReason);

    /**
     * Actualiza el estado de una reclamación por número de reclamación
     */
    ClaimDto updateClaimStatusByNumber(String claimNumber, Claim.ClaimStatus status, String comments, BigDecimal approvedAmount, String denialReason);

    /**
     * Obtiene todas las reclamaciones en un estado específico
     */
    List<ClaimDto> getClaimsByStatus(Claim.ClaimStatus status);

    /**
     * Obtiene reclamaciones que ocurrieron en un rango de fechas
     */
    List<ClaimDto> getClaimsByIncidentDateRange(LocalDate startDate, LocalDate endDate);

    /**
     * Obtiene estadísticas de reclamaciones para dashboard
     */
    Map<String, Object> getClaimsDashboardStatistics();

    /**
     * Realiza un análisis avanzado asíncrono de reclamaciones con múltiples métricas
     */
    CompletableFuture<Map<String, Object>> getClaimsAdvancedAnalyticsAsync();

    /**
     * Añade un ítem a una reclamación por ID (método interno)
     */
    ClaimItemDto addClaimItem(Long claimId, ClaimItemDto itemDto);

    /**
     * Añade un ítem a una reclamación por número de reclamación
     */
    ClaimItemDto addClaimItemByClaimNumber(String claimNumber, ClaimItemDto itemDto);

    /**
     * Obtiene todos los ítems de una reclamación por ID (método interno)
     */
    List<ClaimItemDto> getClaimItems(Long claimId);

    /**
     * Obtiene todos los ítems de una reclamación por número de reclamación
     */
    List<ClaimItemDto> getClaimItemsByClaimNumber(String claimNumber);

    /**
     * Actualiza un ítem de una reclamación
     */
    ClaimItemDto updateClaimItem(String claimNumber, Long itemId, ClaimItemDto itemDto);

    /**
     * Obtiene un resumen de reclamaciones con filtros opcionales
     */
    List<ClaimSummaryDto> getClaimsSummary(Claim.ClaimStatus status, LocalDate from, LocalDate to);

    /**
     * Genera un número de reclamación
     */
    String generateClaimNumber(Claim.ClaimType claimType);
}
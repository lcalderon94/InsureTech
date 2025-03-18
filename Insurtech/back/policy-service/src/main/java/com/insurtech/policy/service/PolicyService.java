package com.insurtech.policy.service;

import com.insurtech.policy.model.dto.PolicyDto;
import com.insurtech.policy.model.dto.PolicyNoteDto;
import com.insurtech.policy.model.entity.Policy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface PolicyService {

    /**
     * Crea una nueva póliza
     */
    PolicyDto createPolicy(PolicyDto policyDto);

    /**
     * Obtiene una póliza por su ID
     */
    Optional<PolicyDto> getPolicyById(Long id);

    /**
     * Obtiene una póliza por su número
     */
    Optional<PolicyDto> getPolicyByNumber(String policyNumber);

    /**
     * Busca pólizas por término de búsqueda
     */
    Page<PolicyDto> searchPolicies(String searchTerm, Pageable pageable);

    /**
     * Obtiene todas las pólizas de un cliente
     */
    List<PolicyDto> getPoliciesByCustomerId(Long customerId);

    /**
     * Obtiene todas las pólizas de un cliente paginadas
     */
    Page<PolicyDto> getPoliciesByCustomerId(Long customerId, Pageable pageable);

    /**
     * Actualiza una póliza existente
     */
    PolicyDto updatePolicy(Long id, PolicyDto policyDto);

    /**
     * Elimina una póliza (solo si está en estado DRAFT)
     */
    void deletePolicy(Long id);

    /**
     * Cambia el estado de una póliza
     */
    PolicyDto updatePolicyStatus(Long id, Policy.PolicyStatus status, String reason);

    /**
     * Añade una nota a una póliza
     */
    PolicyNoteDto addPolicyNote(Long policyId, PolicyNoteDto noteDto);

    /**
     * Obtiene pólizas que expiran en un rango de fechas
     */
    List<PolicyDto> getPoliciesExpiringBetween(LocalDate startDate, LocalDate endDate);

    /**
     * Cálculo asíncrono de prima
     */
    CompletableFuture<Double> calculatePremiumAsync(PolicyDto policyDto);

    /**
     * Cálculo asíncrono de estadísticas de pólizas para un cliente
     */
    CompletableFuture<Map<String, Object>> calculateCustomerPolicyStatisticsAsync(Long customerId);

    /**
     * Genera el número de póliza
     */
    String generatePolicyNumber(Policy.PolicyType policyType);

    /**
     * Valida una póliza de forma concurrente
     */
    CompletableFuture<Boolean> validatePolicyAsync(PolicyDto policyDto);
}
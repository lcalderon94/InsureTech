package com.insurtech.policy.service;

import com.insurtech.policy.model.dto.PolicyDto;
import com.insurtech.policy.model.dto.PolicyVersionDto;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface PolicyVersioningService {

    /**
     * Crea una nueva versión de póliza
     */
    PolicyVersionDto createPolicyVersion(Long policyId, String changeReason);

    /**
     * Obtiene una versión específica de póliza
     */
    Optional<PolicyVersionDto> getPolicyVersion(Long policyId, Integer versionNumber);

    /**
     * Obtiene todas las versiones de una póliza
     */
    List<PolicyVersionDto> getAllPolicyVersions(Long policyId);

    /**
     * Obtiene la última versión de una póliza
     */
    Optional<PolicyVersionDto> getLatestPolicyVersion(Long policyId);

    /**
     * Compara dos versiones de póliza
     */
    Map<String, Object> comparePolicyVersions(Long policyId, Integer versionNumber1, Integer versionNumber2);

    /**
     * Obtiene el historial de cambios de una póliza
     */
    List<Map<String, Object>> getPolicyChangeHistory(Long policyId);

    /**
     * Restaura una póliza a una versión anterior
     */
    PolicyDto restorePolicyVersion(Long policyId, Integer versionNumber, String reason);
}
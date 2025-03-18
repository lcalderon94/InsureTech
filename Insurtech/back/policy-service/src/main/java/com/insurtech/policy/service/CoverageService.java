package com.insurtech.policy.service;

import com.insurtech.policy.model.dto.CoverageDto;
import com.insurtech.policy.model.dto.PolicyCoverageDto;
import com.insurtech.policy.model.entity.Coverage;

import java.util.List;
import java.util.Optional;

public interface CoverageService {

    /**
     * Crea una nueva cobertura
     */
    CoverageDto createCoverage(CoverageDto coverageDto);

    /**
     * Obtiene una cobertura por su ID
     */
    Optional<CoverageDto> getCoverageById(Long id);

    /**
     * Obtiene una cobertura por su código
     */
    Optional<CoverageDto> getCoverageByCode(String code);

    /**
     * Obtiene todas las coberturas activas
     */
    List<CoverageDto> getAllActiveCoverages();

    /**
     * Obtiene todas las coberturas para un tipo de póliza específico
     */
    List<CoverageDto> getCoveragesByPolicyType(String policyType);

    /**
     * Actualiza una cobertura existente
     */
    CoverageDto updateCoverage(Long id, CoverageDto coverageDto);

    /**
     * Activa o desactiva una cobertura
     */
    CoverageDto setCoverageStatus(Long id, boolean active);

    /**
     * Agrega una cobertura a una póliza
     */
    PolicyCoverageDto addCoverageToPolicy(Long policyId, PolicyCoverageDto policyCoverageDto);

    /**
     * Actualiza una cobertura de una póliza
     */
    PolicyCoverageDto updatePolicyCoverage(Long policyId, Long coverageId, PolicyCoverageDto policyCoverageDto);

    /**
     * Elimina una cobertura de una póliza
     */
    void removeCoverageFromPolicy(Long policyId, Long coverageId);

    /**
     * Obtiene todas las coberturas de una póliza
     */
    List<PolicyCoverageDto> getPolicyCoverages(Long policyId);

    /**
     * Busca coberturas por término de búsqueda
     */
    List<CoverageDto> searchCoverages(String searchTerm);
}
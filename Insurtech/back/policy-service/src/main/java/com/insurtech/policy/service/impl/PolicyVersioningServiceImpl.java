package com.insurtech.policy.service.impl;

import com.insurtech.policy.model.dto.PolicyDto;
import com.insurtech.policy.model.dto.PolicyVersionDto;
import com.insurtech.policy.model.entity.Policy;
import com.insurtech.policy.model.entity.PolicyVersion;
import com.insurtech.policy.repository.PolicyRepository;
import com.insurtech.policy.repository.PolicyVersionRepository;
import com.insurtech.policy.service.PolicyVersioningService;
import com.insurtech.policy.util.EntityDtoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PolicyVersioningServiceImpl implements PolicyVersioningService {

    private final PolicyRepository policyRepository;
    private final PolicyVersionRepository policyVersionRepository;
    private final EntityDtoMapper mapper;

    @Override
    @Transactional
    public PolicyVersionDto createPolicyVersion(Long policyId, String changeReason) {
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new NoSuchElementException("Póliza no encontrada con ID: " + policyId));

        // Obtener el siguiente número de versión
        Integer maxVersion = policyVersionRepository.findMaxVersionNumberByPolicyId(policyId);
        int nextVersion = (maxVersion != null) ? maxVersion + 1 : 1;

        // Crear nueva versión
        PolicyVersion version = new PolicyVersion();
        version.setPolicy(policy);
        version.setVersionNumber(nextVersion);
        version.setStatus(policy.getStatus());
        version.setStartDate(policy.getStartDate());
        version.setEndDate(policy.getEndDate());
        version.setPremium(policy.getPremium());
        version.setSumInsured(policy.getSumInsured());
        version.setChangeReason(changeReason);
        version.setCreatedBy(getCurrentUsername());

        // Guardar versión
        version = policyVersionRepository.save(version);

        return mapper.toDto(version);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PolicyVersionDto> getPolicyVersion(Long policyId, Integer versionNumber) {
        return policyVersionRepository.findByPolicyIdAndVersionNumber(policyId, versionNumber)
                .map(mapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PolicyVersionDto> getAllPolicyVersions(Long policyId) {
        return policyVersionRepository.findByPolicyId(policyId).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PolicyVersionDto> getLatestPolicyVersion(Long policyId) {
        List<PolicyVersion> versions = policyVersionRepository.findByPolicyIdOrderByVersionNumberDesc(policyId);

        if (versions.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(mapper.toDto(versions.get(0)));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> comparePolicyVersions(Long policyId, Integer versionNumber1, Integer versionNumber2) {
        Optional<PolicyVersion> version1Opt = policyVersionRepository.findByPolicyIdAndVersionNumber(policyId, versionNumber1);
        Optional<PolicyVersion> version2Opt = policyVersionRepository.findByPolicyIdAndVersionNumber(policyId, versionNumber2);

        if (version1Opt.isEmpty() || version2Opt.isEmpty()) {
            throw new NoSuchElementException("Una o ambas versiones no existen");
        }

        PolicyVersion version1 = version1Opt.get();
        PolicyVersion version2 = version2Opt.get();

        Map<String, Object> differences = new HashMap<>();

        // Comparar campos relevantes
        if (!Objects.equals(version1.getStatus(), version2.getStatus())) {
            differences.put("status", Map.of(
                    "version" + versionNumber1, version1.getStatus(),
                    "version" + versionNumber2, version2.getStatus()
            ));
        }

        // Otras comparaciones similares...

        return differences;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getPolicyChangeHistory(Long policyId) {
        List<PolicyVersion> versions = policyVersionRepository.findByPolicyIdOrderByVersionNumberDesc(policyId);

        List<Map<String, Object>> changeHistory = new ArrayList<>();
        for (PolicyVersion version : versions) {
            Map<String, Object> change = new HashMap<>();
            change.put("versionNumber", version.getVersionNumber());
            change.put("status", version.getStatus());
            change.put("changeReason", version.getChangeReason());
            change.put("createdAt", version.getCreatedAt());
            change.put("createdBy", version.getCreatedBy());

            changeHistory.add(change);
        }

        return changeHistory;
    }

    @Override
    @Transactional
    public PolicyDto restorePolicyVersion(Long policyId, Integer versionNumber, String reason) {
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new NoSuchElementException("Póliza no encontrada con ID: " + policyId));

        PolicyVersion version = policyVersionRepository.findByPolicyIdAndVersionNumber(policyId, versionNumber)
                .orElseThrow(() -> new NoSuchElementException("Versión no encontrada: " + versionNumber));

        // Restaurar campos desde la versión
        policy.setStatus(version.getStatus());
        policy.setStartDate(version.getStartDate());
        policy.setEndDate(version.getEndDate());
        policy.setPremium(version.getPremium());
        policy.setSumInsured(version.getSumInsured());

        policy = policyRepository.save(policy);

        // Crear nueva versión para registrar la restauración
        createPolicyVersion(policyId, "Restaurado a versión " + versionNumber + ": " + reason);

        return mapper.toDto(policy);
    }

    private String getCurrentUsername() {
        try {
            return org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        } catch (Exception e) {
            return "system";
        }
    }
}
package com.insurtech.policy.util;

import com.insurtech.policy.model.dto.*;
import com.insurtech.policy.model.entity.*;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class EntityDtoMapper {

    public PolicyDto toDto(Policy entity) {
        if (entity == null) return null;

        PolicyDto dto = new PolicyDto();
        dto.setId(entity.getId());
        dto.setPolicyNumber(entity.getPolicyNumber());
        dto.setCustomerId(entity.getCustomerId());
        dto.setCustomerNumber(entity.getCustomerNumber());
        dto.setPolicyType(entity.getPolicyType());
        dto.setStatus(entity.getStatus());
        dto.setStartDate(entity.getStartDate());
        dto.setEndDate(entity.getEndDate());
        dto.setIssueDate(entity.getIssueDate());
        dto.setPremium(entity.getPremium());
        dto.setSumInsured(entity.getSumInsured());
        dto.setPaymentFrequency(entity.getPaymentFrequency());
        dto.setPaymentMethod(entity.getPaymentMethod());
        dto.setDescription(entity.getDescription());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());

        // Mapear coberturas
        if (entity.getCoverages() != null) {
            dto.setCoverages(entity.getCoverages().stream()
                    .map(this::toDto)
                    .collect(Collectors.toSet()));
        }

        // Mapear notas
        if (entity.getNotes() != null) {
            dto.setNotes(entity.getNotes().stream()
                    .map(this::toDto)
                    .collect(Collectors.toSet()));
        }

        return dto;
    }

    public Policy toEntity(PolicyDto dto) {
        if (dto == null) return null;

        Policy entity = new Policy();
        entity.setId(dto.getId());
        entity.setPolicyNumber(dto.getPolicyNumber());
        entity.setCustomerId(dto.getCustomerId());
        entity.setCustomerNumber(dto.getCustomerNumber());
        entity.setPolicyType(dto.getPolicyType());
        entity.setStatus(dto.getStatus());
        entity.setStartDate(dto.getStartDate());
        entity.setEndDate(dto.getEndDate());
        entity.setIssueDate(dto.getIssueDate());
        entity.setPremium(dto.getPremium());
        entity.setSumInsured(dto.getSumInsured());
        entity.setPaymentFrequency(dto.getPaymentFrequency());
        entity.setPaymentMethod(dto.getPaymentMethod());
        entity.setDescription(dto.getDescription());

        return entity;
    }

    public PolicyCoverageDto toDto(PolicyCoverage entity) {
        if (entity == null) return null;

        PolicyCoverageDto dto = new PolicyCoverageDto();
        dto.setId(entity.getId());
        dto.setPolicyId(entity.getPolicy().getId());
        dto.setCoverageId(entity.getCoverage().getId());
        dto.setCoverageCode(entity.getCoverage().getCode());
        dto.setCoverageName(entity.getCoverage().getName());
        dto.setSumInsured(entity.getSumInsured());
        dto.setPremium(entity.getPremium());
        dto.setPremiumRate(entity.getPremiumRate());
        dto.setDeductible(entity.getDeductible());
        dto.setDeductibleType(entity.getDeductibleType());
        dto.setMandatory(entity.isMandatory());
        dto.setAdditionalData(entity.getAdditionalData());

        return dto;
    }

    public PolicyCoverage toEntity(PolicyCoverageDto dto) {
        if (dto == null) return null;

        PolicyCoverage entity = new PolicyCoverage();
        entity.setId(dto.getId());
        entity.setSumInsured(dto.getSumInsured());
        entity.setPremium(dto.getPremium());
        entity.setPremiumRate(dto.getPremiumRate());
        entity.setDeductible(dto.getDeductible());
        entity.setDeductibleType(dto.getDeductibleType());
        entity.setMandatory(dto.isMandatory());
        entity.setAdditionalData(dto.getAdditionalData());

        return entity;
    }

    public PolicyNoteDto toDto(PolicyNote entity) {
        if (entity == null) return null;

        PolicyNoteDto dto = new PolicyNoteDto();
        dto.setId(entity.getId());
        dto.setPolicyId(entity.getPolicy().getId());
        dto.setNoteType(entity.getNoteType());
        dto.setTitle(entity.getTitle());
        dto.setContent(entity.getContent());
        dto.setImportant(entity.isImportant());
        dto.setSystemGenerated(entity.isSystemGenerated());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setCreatedBy(entity.getCreatedBy());

        return dto;
    }

    public PolicyNote toEntity(PolicyNoteDto dto) {
        if (dto == null) return null;

        PolicyNote entity = new PolicyNote();
        entity.setId(dto.getId());
        entity.setNoteType(dto.getNoteType());
        entity.setTitle(dto.getTitle());
        entity.setContent(dto.getContent());
        entity.setImportant(dto.isImportant());
        entity.setSystemGenerated(dto.isSystemGenerated());

        return entity;
    }

    public CoverageDto toDto(Coverage entity) {
        if (entity == null) return null;

        CoverageDto dto = new CoverageDto();
        dto.setId(entity.getId());
        dto.setCode(entity.getCode());
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setCoverageType(entity.getCoverageType());
        dto.setDefaultPremiumRate(entity.getDefaultPremiumRate());
        dto.setDefaultSumInsured(entity.getDefaultSumInsured());
        dto.setMinimumSumInsured(entity.getMinimumSumInsured());
        dto.setMaximumSumInsured(entity.getMaximumSumInsured());
        dto.setActive(entity.isActive());
        dto.setPolicyTypes(entity.getPolicyTypes());

        return dto;
    }

    public Coverage toEntity(CoverageDto dto) {
        if (dto == null) return null;

        Coverage entity = new Coverage();
        entity.setId(dto.getId());
        entity.setCode(dto.getCode());
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setCoverageType(dto.getCoverageType());
        entity.setDefaultPremiumRate(dto.getDefaultPremiumRate());
        entity.setDefaultSumInsured(dto.getDefaultSumInsured());
        entity.setMinimumSumInsured(dto.getMinimumSumInsured());
        entity.setMaximumSumInsured(dto.getMaximumSumInsured());
        entity.setActive(dto.isActive());
        entity.setPolicyTypes(dto.getPolicyTypes());

        return entity;
    }

    public PolicyVersionDto toDto(PolicyVersion entity) {
        if (entity == null) return null;

        PolicyVersionDto dto = new PolicyVersionDto();
        dto.setId(entity.getId());
        dto.setPolicyId(entity.getPolicy().getId());
        dto.setVersionNumber(entity.getVersionNumber());
        dto.setStatus(entity.getStatus());
        dto.setStartDate(entity.getStartDate());
        dto.setEndDate(entity.getEndDate());
        dto.setPremium(entity.getPremium());
        dto.setSumInsured(entity.getSumInsured());
        dto.setChangeReason(entity.getChangeReason());
        dto.setPolicyData(entity.getPolicyData());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setCreatedBy(entity.getCreatedBy());

        return dto;
    }
}
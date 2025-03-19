package com.insurtech.quote.util;

import com.insurtech.quote.model.dto.QuoteCoverageDto;
import com.insurtech.quote.model.dto.QuoteDto;
import com.insurtech.quote.model.dto.QuoteOptionDto;
import com.insurtech.quote.model.entity.Quote;
import com.insurtech.quote.model.entity.QuoteCoverage;
import com.insurtech.quote.model.entity.QuoteOption;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.stream.Collectors;

/**
 * Utilidad para la conversión entre entidades y DTOs
 */
@Component
public class EntityDtoMapper {

    /**
     * Convierte una entidad Quote a DTO
     */
    public QuoteDto toDto(Quote entity) {
        if (entity == null) return null;

        QuoteDto dto = new QuoteDto();
        dto.setId(entity.getId());
        dto.setQuoteNumber(entity.getQuoteNumber());
        dto.setCustomerId(entity.getCustomerId());
        dto.setCustomerNumber(entity.getCustomerNumber());
        dto.setQuoteType(entity.getQuoteType());
        dto.setStatus(entity.getStatus());
        dto.setPremium(entity.getPremium());
        dto.setSumInsured(entity.getSumInsured());
        dto.setRiskDetails(entity.getRiskDetails());
        dto.setEffectiveFrom(entity.getEffectiveFrom());
        dto.setEffectiveTo(entity.getEffectiveTo());
        dto.setPaymentFrequency(entity.getPaymentFrequency());
        dto.setAdditionalInformation(entity.getAdditionalInformation());
        dto.setValidUntil(entity.getValidUntil());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());

        // Mappear coberturas
        if (entity.getCoverages() != null) {
            dto.setCoverages(entity.getCoverages().stream()
                    .map(this::toDto)
                    .collect(Collectors.toSet()));
        }

        // Mappear opciones
        if (entity.getOptions() != null) {
            dto.setOptions(entity.getOptions().stream()
                    .map(this::toDto)
                    .collect(Collectors.toSet()));
        }

        return dto;
    }

    /**
     * Convierte un DTO QuoteDto a entidad
     */
    public Quote toEntity(QuoteDto dto) {
        if (dto == null) return null;

        Quote entity = new Quote();
        entity.setId(dto.getId());
        entity.setQuoteNumber(dto.getQuoteNumber());
        entity.setCustomerId(dto.getCustomerId());
        entity.setCustomerNumber(dto.getCustomerNumber());
        entity.setQuoteType(dto.getQuoteType());
        entity.setStatus(dto.getStatus());
        entity.setPremium(dto.getPremium());
        entity.setSumInsured(dto.getSumInsured());
        entity.setRiskDetails(dto.getRiskDetails());
        entity.setEffectiveFrom(dto.getEffectiveFrom());
        entity.setEffectiveTo(dto.getEffectiveTo());
        entity.setPaymentFrequency(dto.getPaymentFrequency());
        entity.setAdditionalInformation(dto.getAdditionalInformation());
        entity.setValidUntil(dto.getValidUntil());

        // Inicializar colecciones
        entity.setCoverages(new HashSet<>());
        entity.setOptions(new HashSet<>());

        // Mappear coberturas
        if (dto.getCoverages() != null) {
            entity.setCoverages(dto.getCoverages().stream()
                    .map(this::toEntity)
                    .collect(Collectors.toSet()));
        }

        // Mappear opciones
        if (dto.getOptions() != null) {
            entity.setOptions(dto.getOptions().stream()
                    .map(this::toEntity)
                    .collect(Collectors.toSet()));
        }

        return entity;
    }

    /**
     * Convierte una entidad QuoteCoverage a DTO
     */
    public QuoteCoverageDto toDto(QuoteCoverage entity) {
        if (entity == null) return null;

        QuoteCoverageDto dto = new QuoteCoverageDto();
        dto.setCoverageId(entity.getCoverageId());
        dto.setCoverageCode(entity.getCoverageCode());
        dto.setCoverageName(entity.getCoverageName());
        dto.setSumInsured(entity.getSumInsured());
        dto.setPremium(entity.getPremium());
        dto.setRate(entity.getRate());
        dto.setDeductible(entity.getDeductible());
        dto.setDeductibleType(entity.getDeductibleType());
        dto.setMandatory(entity.isMandatory());
        dto.setAdditionalData(entity.getAdditionalData());

        return dto;
    }

    /**
     * Convierte un DTO QuoteCoverageDto a entidad
     */
    public QuoteCoverage toEntity(QuoteCoverageDto dto) {
        if (dto == null) return null;

        QuoteCoverage entity = new QuoteCoverage();
        entity.setCoverageId(dto.getCoverageId());
        entity.setCoverageCode(dto.getCoverageCode());
        entity.setCoverageName(dto.getCoverageName());
        entity.setSumInsured(dto.getSumInsured());
        entity.setPremium(dto.getPremium());
        entity.setRate(dto.getRate());
        entity.setDeductible(dto.getDeductible());
        entity.setDeductibleType(dto.getDeductibleType());
        entity.setMandatory(dto.isMandatory());
        entity.setAdditionalData(dto.getAdditionalData());

        return entity;
    }

    /**
     * Convierte una entidad QuoteOption a DTO
     */
    public QuoteOptionDto toDto(QuoteOption entity) {
        if (entity == null) return null;

        QuoteOptionDto dto = new QuoteOptionDto();
        dto.setOptionId(entity.getOptionId());
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setPremium(entity.getPremium());
        dto.setSumInsured(entity.getSumInsured());
        dto.setAdditionalData(entity.getAdditionalData());
        dto.setDisplayOrder(entity.getDisplayOrder());
        dto.setRecommended(entity.isRecommended());

        // Mappear coberturas de la opción
        if (entity.getCoverages() != null) {
            dto.setCoverages(entity.getCoverages().stream()
                    .map(this::toDto)
                    .collect(Collectors.toSet()));
        }

        return dto;
    }

    /**
     * Convierte un DTO QuoteOptionDto a entidad
     */
    public QuoteOption toEntity(QuoteOptionDto dto) {
        if (dto == null) return null;

        QuoteOption entity = new QuoteOption();
        entity.setOptionId(dto.getOptionId());
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setPremium(dto.getPremium());
        entity.setSumInsured(dto.getSumInsured());
        entity.setAdditionalData(dto.getAdditionalData());
        entity.setDisplayOrder(dto.getDisplayOrder());
        entity.setRecommended(dto.isRecommended());

        // Inicializar colección de coberturas
        entity.setCoverages(new HashSet<>());

        // Mappear coberturas de la opción
        if (dto.getCoverages() != null) {
            entity.setCoverages(dto.getCoverages().stream()
                    .map(this::toEntity)
                    .collect(Collectors.toSet()));
        }

        return entity;
    }
}
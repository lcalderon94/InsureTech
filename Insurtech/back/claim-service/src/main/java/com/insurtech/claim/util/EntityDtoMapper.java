package com.insurtech.claim.util;

import com.insurtech.claim.model.dto.ClaimDto;
import com.insurtech.claim.model.dto.ClaimDocumentDto;
import com.insurtech.claim.model.dto.ClaimItemDto;
import com.insurtech.claim.model.entity.Claim;
import com.insurtech.claim.model.entity.ClaimDocument;
import com.insurtech.claim.model.entity.ClaimItem;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class EntityDtoMapper {

    /**
     * Convert Claim entity to ClaimDto
     */
    public ClaimDto toDto(Claim entity) {
        if (entity == null) {
            return null;
        }

        ClaimDto dto = new ClaimDto();
        dto.setId(entity.getId());
        dto.setClaimNumber(entity.getClaimNumber());
        dto.setPolicyId(entity.getPolicyId());
        dto.setPolicyNumber(entity.getPolicyNumber());
        dto.setCustomerId(entity.getCustomerId());
        dto.setCustomerNumber(entity.getCustomerNumber());
        dto.setIncidentDate(entity.getIncidentDate());
        dto.setIncidentDescription(entity.getIncidentDescription());
        dto.setStatus(entity.getStatus());
        dto.setClaimType(entity.getClaimType());
        dto.setEstimatedAmount(entity.getEstimatedAmount());
        dto.setApprovedAmount(entity.getApprovedAmount());
        dto.setPaidAmount(entity.getPaidAmount());
        dto.setDenialReason(entity.getDenialReason());
        dto.setHandlerComments(entity.getHandlerComments());
        dto.setCustomerContactInfo(entity.getCustomerContactInfo());
        dto.setSubmissionDate(entity.getSubmissionDate());
        dto.setAssessmentDate(entity.getAssessmentDate());
        dto.setApprovalDate(entity.getApprovalDate());
        dto.setSettlementDate(entity.getSettlementDate());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());

        // Map claim items
        if (entity.getItems() != null && !entity.getItems().isEmpty()) {
            dto.setItems(entity.getItems().stream()
                    .map(this::toDto)
                    .collect(Collectors.toSet()));
        } else {
            dto.setItems(new HashSet<>());
        }

        // Map claim documents
        if (entity.getDocuments() != null && !entity.getDocuments().isEmpty()) {
            dto.setDocuments(entity.getDocuments().stream()
                    .map(this::toDto)
                    .collect(Collectors.toSet()));
        } else {
            dto.setDocuments(new HashSet<>());
        }

        return dto;
    }

    /**
     * Convert ClaimDto to Claim entity
     */
    public Claim toEntity(ClaimDto dto) {
        if (dto == null) {
            return null;
        }

        Claim entity = new Claim();
        entity.setId(dto.getId());
        entity.setClaimNumber(dto.getClaimNumber());
        entity.setPolicyId(dto.getPolicyId());
        entity.setPolicyNumber(dto.getPolicyNumber());
        entity.setCustomerId(dto.getCustomerId());
        entity.setCustomerNumber(dto.getCustomerNumber());
        entity.setIncidentDate(dto.getIncidentDate());
        entity.setIncidentDescription(dto.getIncidentDescription());
        entity.setStatus(dto.getStatus());
        entity.setClaimType(dto.getClaimType());
        entity.setEstimatedAmount(dto.getEstimatedAmount());
        entity.setApprovedAmount(dto.getApprovedAmount());
        entity.setPaidAmount(dto.getPaidAmount());
        entity.setDenialReason(dto.getDenialReason());
        entity.setHandlerComments(dto.getHandlerComments());
        entity.setCustomerContactInfo(dto.getCustomerContactInfo());
        entity.setSubmissionDate(dto.getSubmissionDate());
        entity.setAssessmentDate(dto.getAssessmentDate());
        entity.setApprovalDate(dto.getApprovalDate());
        entity.setSettlementDate(dto.getSettlementDate());

        // Map claim items - bidirectional relationship needs special handling
        if (dto.getItems() != null && !dto.getItems().isEmpty()) {
            Set<ClaimItem> items = dto.getItems().stream()
                    .map(this::toEntity)
                    .collect(Collectors.toSet());

            // Set bidirectional relationship
            for (ClaimItem item : items) {
                item.setClaim(entity);
            }

            entity.setItems(items);
        } else {
            entity.setItems(Collections.emptySet());
        }

        // Map claim documents - bidirectional relationship needs special handling
        if (dto.getDocuments() != null && !dto.getDocuments().isEmpty()) {
            Set<ClaimDocument> documents = dto.getDocuments().stream()
                    .map(this::toEntity)
                    .collect(Collectors.toSet());

            // Set bidirectional relationship
            for (ClaimDocument document : documents) {
                document.setClaim(entity);
            }

            entity.setDocuments(documents);
        } else {
            entity.setDocuments(Collections.emptySet());
        }

        return entity;
    }

    /**
     * Convert ClaimDocument entity to ClaimDocumentDto
     */
    public ClaimDocumentDto toDto(ClaimDocument entity) {
        if (entity == null) {
            return null;
        }

        ClaimDocumentDto dto = new ClaimDocumentDto();
        dto.setId(entity.getId());
        dto.setClaimId(entity.getClaim() != null ? entity.getClaim().getId() : null);
        dto.setTitle(entity.getTitle());
        dto.setDescription(entity.getDescription());
        dto.setDocumentType(entity.getDocumentType());
        dto.setFileName(entity.getFileName());
        dto.setFilePath(entity.getFilePath());
        dto.setMimeType(entity.getMimeType());
        dto.setFileSize(entity.getFileSize());
        dto.setDocumentId(entity.getDocumentId());
        dto.setExternalUrl(entity.getExternalUrl());
        dto.setVerified(entity.isVerified());
        dto.setUploadDate(entity.getUploadDate());
        dto.setUploadedBy(entity.getUploadedBy());

        return dto;
    }

    /**
     * Convert ClaimDocumentDto to ClaimDocument entity
     */
    public ClaimDocument toEntity(ClaimDocumentDto dto) {
        if (dto == null) {
            return null;
        }

        ClaimDocument entity = new ClaimDocument();
        entity.setId(dto.getId());
        entity.setTitle(dto.getTitle());
        entity.setDescription(dto.getDescription());
        entity.setDocumentType(dto.getDocumentType());
        entity.setFileName(dto.getFileName());
        entity.setFilePath(dto.getFilePath());
        entity.setMimeType(dto.getMimeType());
        entity.setFileSize(dto.getFileSize());
        entity.setDocumentId(dto.getDocumentId());
        entity.setExternalUrl(dto.getExternalUrl());
        entity.setVerified(dto.isVerified());
        entity.setUploadDate(dto.getUploadDate());
        entity.setUploadedBy(dto.getUploadedBy());

        // Note: The claim relationship should be set by the service

        return entity;
    }

    /**
     * Convert ClaimItem entity to ClaimItemDto
     */
    public ClaimItemDto toDto(ClaimItem entity) {
        if (entity == null) {
            return null;
        }

        ClaimItemDto dto = new ClaimItemDto();
        dto.setId(entity.getId());
        dto.setClaimId(entity.getClaim() != null ? entity.getClaim().getId() : null);
        dto.setDescription(entity.getDescription());
        dto.setCategory(entity.getCategory());
        dto.setClaimedAmount(entity.getClaimedAmount());
        dto.setApprovedAmount(entity.getApprovedAmount());
        dto.setRejectionReason(entity.getRejectionReason());
        dto.setCovered(entity.isCovered());
        dto.setEvidenceDocumentId(entity.getEvidenceDocumentId());
        dto.setAdditionalDetails(entity.getAdditionalDetails());

        return dto;
    }

    /**
     * Convert ClaimItemDto to ClaimItem entity
     */
    public ClaimItem toEntity(ClaimItemDto dto) {
        if (dto == null) {
            return null;
        }

        ClaimItem entity = new ClaimItem();
        entity.setId(dto.getId());
        entity.setDescription(dto.getDescription());
        entity.setCategory(dto.getCategory());
        entity.setClaimedAmount(dto.getClaimedAmount());
        entity.setApprovedAmount(dto.getApprovedAmount());
        entity.setRejectionReason(dto.getRejectionReason());
        entity.setCovered(dto.isCovered());
        entity.setEvidenceDocumentId(dto.getEvidenceDocumentId());
        entity.setAdditionalDetails(dto.getAdditionalDetails());

        // Note: The claim relationship should be set by the service

        return entity;
    }
}
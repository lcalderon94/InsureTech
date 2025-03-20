package com.insurtech.claim.service;

import com.insurtech.claim.model.dto.ClaimDocumentDto;
import com.insurtech.claim.model.entity.ClaimDocument;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public interface ClaimDocumentService {

    /**
     * Carga un documento para una reclamación por ID (método interno)
     */
    ClaimDocumentDto uploadDocument(
            Long claimId,
            MultipartFile file,
            String title,
            String description,
            ClaimDocument.DocumentType documentType) throws IOException;

    /**
     * Carga un documento para una reclamación por número de reclamación
     */
    ClaimDocumentDto uploadDocumentByClaimNumber(
            String claimNumber,
            MultipartFile file,
            String title,
            String description,
            ClaimDocument.DocumentType documentType) throws IOException;

    /**
     * Obtiene un documento por su ID (método interno)
     */
    Optional<ClaimDocumentDto> getDocumentById(Long documentId);

    /**
     * Obtiene un documento por número de reclamación y título
     */
    Optional<ClaimDocumentDto> getDocumentByClaimNumberAndTitle(String claimNumber, String title);

    /**
     * Obtiene todos los documentos de una reclamación por ID (método interno)
     */
    List<ClaimDocumentDto> getClaimDocuments(Long claimId);

    /**
     * Obtiene todos los documentos de una reclamación por número de reclamación
     */
    List<ClaimDocumentDto> getDocumentsByClaimNumber(String claimNumber);

    /**
     * Descarga un documento por ID (método interno)
     */
    byte[] downloadDocument(Long documentId) throws IOException;

    /**
     * Descarga un documento por número de reclamación y título
     */
    byte[] downloadDocumentByClaimNumberAndTitle(String claimNumber, String title) throws IOException;

    /**
     * Actualiza la información de un documento por ID (método interno)
     */
    ClaimDocumentDto updateDocument(Long documentId, ClaimDocumentDto documentDto);

    /**
     * Actualiza la información de un documento por número de reclamación y título
     */
    ClaimDocumentDto updateDocumentByClaimNumberAndTitle(String claimNumber, String title, ClaimDocumentDto documentDto);

    /**
     * Elimina un documento por ID (método interno)
     */
    void deleteDocument(Long documentId);

    /**
     * Elimina un documento por número de reclamación y título
     */
    void deleteDocumentByClaimNumberAndTitle(String claimNumber, String title);

    /**
     * Marca un documento como verificado o no verificado por ID (método interno)
     */
    ClaimDocumentDto setDocumentVerificationStatus(Long documentId, boolean verified);

    /**
     * Marca un documento como verificado o no verificado por número de reclamación y título
     */
    ClaimDocumentDto setDocumentVerificationStatusByClaimNumberAndTitle(String claimNumber, String title, boolean verified);

    /**
     * Obtiene documentos de un tipo específico
     */
    List<ClaimDocumentDto> getDocumentsByType(ClaimDocument.DocumentType documentType);
}
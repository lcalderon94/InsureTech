package com.insurtech.claim.service;

import com.insurtech.claim.model.dto.ClaimDocumentDto;
import com.insurtech.claim.model.entity.ClaimDocument;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public interface ClaimDocumentService {

    /**
     * Carga un documento para una reclamación
     */
    ClaimDocumentDto uploadDocument(
            Long claimId,
            MultipartFile file,
            String title,
            String description,
            ClaimDocument.DocumentType documentType) throws IOException;

    /**
     * Obtiene un documento por su ID
     */
    Optional<ClaimDocumentDto> getDocumentById(Long documentId);

    /**
     * Obtiene todos los documentos de una reclamación
     */
    List<ClaimDocumentDto> getClaimDocuments(Long claimId);

    /**
     * Descarga un documento
     */
    byte[] downloadDocument(Long documentId) throws IOException;

    /**
     * Actualiza la información de un documento
     */
    ClaimDocumentDto updateDocument(Long documentId, ClaimDocumentDto documentDto);

    /**
     * Elimina un documento
     */
    void deleteDocument(Long documentId);

    /**
     * Marca un documento como verificado o no verificado
     */
    ClaimDocumentDto setDocumentVerificationStatus(Long documentId, boolean verified);

    /**
     * Obtiene documentos de un tipo específico
     */
    List<ClaimDocumentDto> getDocumentsByType(ClaimDocument.DocumentType documentType);
}
package com.insurtech.claim.repository;

import com.insurtech.claim.model.entity.ClaimDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClaimDocumentRepository extends JpaRepository<ClaimDocument, Long> {

    /**
     * Encuentra documentos por ID de reclamación
     */
    List<ClaimDocument> findByClaimId(Long claimId);

    /**
     * Encuentra documentos por ID de reclamación y tipo de documento
     */
    List<ClaimDocument> findByClaimIdAndDocumentType(Long claimId, ClaimDocument.DocumentType documentType);

    /**
     * Encuentra documentos por ID de documento externo
     */
    List<ClaimDocument> findByDocumentId(String documentId);

    /**
     * Encuentra documentos verificados
     */
    List<ClaimDocument> findByVerifiedTrue();

    /**
     * Encuentra documentos verificados por ID de reclamación
     */
    List<ClaimDocument> findByClaimIdAndVerifiedTrue(Long claimId);

    /**
     * Encuentra documentos por número de reclamación
     */
    @Query("SELECT d FROM ClaimDocument d WHERE d.claim.claimNumber = :claimNumber")
    List<ClaimDocument> findByClaimNumber(@Param("claimNumber") String claimNumber);

    /**
     * Encuentra documento por número de reclamación y título
     */
    @Query("SELECT d FROM ClaimDocument d WHERE d.claim.claimNumber = :claimNumber AND d.title = :title")
    Optional<ClaimDocument> findByClaimNumberAndTitle(
            @Param("claimNumber") String claimNumber,
            @Param("title") String title);

    /**
     * Encuentra documentos por número de reclamación y tipo de documento
     */
    @Query("SELECT d FROM ClaimDocument d WHERE d.claim.claimNumber = :claimNumber AND d.documentType = :documentType")
    List<ClaimDocument> findByClaimNumberAndDocumentType(
            @Param("claimNumber") String claimNumber,
            @Param("documentType") ClaimDocument.DocumentType documentType);

    /**
     * Encuentra documentos verificados por número de reclamación
     */
    @Query("SELECT d FROM ClaimDocument d WHERE d.claim.claimNumber = :claimNumber AND d.verified = true")
    List<ClaimDocument> findByClaimNumberAndVerifiedTrue(@Param("claimNumber") String claimNumber);

    /**
     * Encuentra documentos por tipo de documento
     */
    List<ClaimDocument> findByDocumentType(ClaimDocument.DocumentType documentType);
}
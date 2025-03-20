package com.insurtech.claim.repository;

import com.insurtech.claim.model.entity.ClaimDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClaimDocumentRepository extends JpaRepository<ClaimDocument, Long> {

    List<ClaimDocument> findByClaimId(Long claimId);

    List<ClaimDocument> findByClaimIdAndDocumentType(Long claimId, ClaimDocument.DocumentType documentType);

    List<ClaimDocument> findByDocumentId(String documentId);

    List<ClaimDocument> findByVerifiedTrue();

    List<ClaimDocument> findByClaimIdAndVerifiedTrue(Long claimId);
}
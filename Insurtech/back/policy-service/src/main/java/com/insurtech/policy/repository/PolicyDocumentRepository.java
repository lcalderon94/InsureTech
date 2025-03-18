package com.insurtech.policy.repository;

import com.insurtech.policy.model.entity.PolicyDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PolicyDocumentRepository extends JpaRepository<PolicyDocument, Long> {

    List<PolicyDocument> findByPolicyId(Long policyId);

    List<PolicyDocument> findByPolicyIdAndDocumentType(Long policyId, PolicyDocument.DocumentType documentType);

    List<PolicyDocument> findByDocumentId(String documentId);
}
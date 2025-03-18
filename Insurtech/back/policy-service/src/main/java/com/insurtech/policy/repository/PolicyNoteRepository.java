package com.insurtech.policy.repository;

import com.insurtech.policy.model.entity.PolicyNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PolicyNoteRepository extends JpaRepository<PolicyNote, Long> {

    List<PolicyNote> findByPolicyId(Long policyId);

    List<PolicyNote> findByPolicyIdAndNoteType(Long policyId, PolicyNote.NoteType noteType);

    List<PolicyNote> findByPolicyIdAndIsImportantTrue(Long policyId);

    List<PolicyNote> findByPolicyIdOrderByCreatedAtDesc(Long policyId);
}
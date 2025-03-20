package com.insurtech.claim.repository;

import com.insurtech.claim.model.entity.ClaimNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClaimNoteRepository extends JpaRepository<ClaimNote, Long> {

    List<ClaimNote> findByClaimId(Long claimId);

    List<ClaimNote> findByClaimIdAndNoteType(Long claimId, ClaimNote.NoteType noteType);

    List<ClaimNote> findByClaimIdAndIsImportantTrue(Long claimId);

    List<ClaimNote> findByClaimIdAndIsInternalTrue(Long claimId);

    List<ClaimNote> findByClaimIdAndIsInternalFalse(Long claimId);

    List<ClaimNote> findByClaimIdOrderByCreatedAtDesc(Long claimId);
}
package com.insurtech.claim.repository;

import com.insurtech.claim.model.entity.ClaimEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ClaimEventRepository extends JpaRepository<ClaimEvent, Long> {

    List<ClaimEvent> findByClaimId(Long claimId);

    List<ClaimEvent> findByClaimIdAndEventType(Long claimId, ClaimEvent.EventType eventType);

    List<ClaimEvent> findByClaimIdOrderByCreatedAtDesc(Long claimId);

    List<ClaimEvent> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}
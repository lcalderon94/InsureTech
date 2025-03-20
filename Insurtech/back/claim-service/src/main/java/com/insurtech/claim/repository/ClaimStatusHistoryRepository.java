package com.insurtech.claim.repository;

import com.insurtech.claim.model.entity.ClaimStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ClaimStatusHistoryRepository extends JpaRepository<ClaimStatusHistory, Long> {

    List<ClaimStatusHistory> findByClaimId(Long claimId);

    List<ClaimStatusHistory> findByClaimIdOrderByCreatedAtDesc(Long claimId);

    @Query("SELECT h FROM ClaimStatusHistory h WHERE h.claim.id = :claimId AND h.newStatus = :status ORDER BY h.createdAt ASC")
    List<ClaimStatusHistory> findFirstTimeStatusReached(
            @Param("claimId") Long claimId,
            @Param("status") com.insurtech.claim.model.entity.Claim.ClaimStatus status);

    @Query("SELECT AVG(DATEDIFF(day, h1.createdAt, h2.createdAt)) FROM ClaimStatusHistory h1 " +
            "JOIN ClaimStatusHistory h2 ON h1.claim.id = h2.claim.id " +
            "WHERE h1.newStatus = :statusFrom AND h2.newStatus = :statusTo " +
            "AND h1.createdAt < h2.createdAt " +
            "AND NOT EXISTS (SELECT 1 FROM ClaimStatusHistory h3 " +
            "                WHERE h3.claim.id = h1.claim.id " +
            "                AND h3.createdAt > h1.createdAt " +
            "                AND h3.createdAt < h2.createdAt)")
    Double getAverageTransitionTime(
            @Param("statusFrom") com.insurtech.claim.model.entity.Claim.ClaimStatus statusFrom,
            @Param("statusTo") com.insurtech.claim.model.entity.Claim.ClaimStatus statusTo);
}
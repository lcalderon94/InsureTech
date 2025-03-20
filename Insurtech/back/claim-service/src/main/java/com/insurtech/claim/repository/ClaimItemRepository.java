package com.insurtech.claim.repository;

import com.insurtech.claim.model.entity.ClaimItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ClaimItemRepository extends JpaRepository<ClaimItem, Long> {

    List<ClaimItem> findByClaimId(Long claimId);

    List<ClaimItem> findByClaimIdAndCovered(Long claimId, boolean covered);

    List<ClaimItem> findByCategory(String category);

    @Query("SELECT SUM(i.approvedAmount) FROM ClaimItem i WHERE i.claim.id = :claimId")
    BigDecimal sumApprovedAmountsByClaimId(@Param("claimId") Long claimId);

    @Query("SELECT SUM(i.claimedAmount) FROM ClaimItem i WHERE i.claim.id = :claimId")
    BigDecimal sumClaimedAmountsByClaimId(@Param("claimId") Long claimId);

    @Query("SELECT i FROM ClaimItem i WHERE i.claim.id = :claimId AND i.claimedAmount > :amount")
    List<ClaimItem> findHighValueItemsForClaim(
            @Param("claimId") Long claimId,
            @Param("amount") BigDecimal amount);
}
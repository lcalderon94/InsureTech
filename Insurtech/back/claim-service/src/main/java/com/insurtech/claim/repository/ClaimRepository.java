package com.insurtech.claim.repository;

import com.insurtech.claim.model.entity.Claim;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClaimRepository extends JpaRepository<Claim, Long> {

    Optional<Claim> findByClaimNumber(String claimNumber);

    List<Claim> findByCustomerId(Long customerId);

    List<Claim> findByPolicyId(Long policyId);

    List<Claim> findByPolicyNumber(String policyNumber);

    List<Claim> findByCustomerNumber(String customerNumber);

    List<Claim> findByStatus(Claim.ClaimStatus status);

    List<Claim> findByClaimType(Claim.ClaimType claimType);

    List<Claim> findByStatusAndClaimType(Claim.ClaimStatus status, Claim.ClaimType claimType);

    List<Claim> findByIncidentDateBetween(LocalDate startDate, LocalDate endDate);

    @Query("SELECT c FROM Claim c WHERE " +
            "LOWER(c.claimNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(c.policyNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(c.customerNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(c.incidentDescription) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<Claim> search(@Param("searchTerm") String searchTerm, Pageable pageable);

    @Query("SELECT c FROM Claim c WHERE " +
            "c.status IN :statuses AND " +
            "c.updatedAt < :cutoffDate")
    List<Claim> findByStatusInAndNotUpdatedSince(
            @Param("statuses") List<Claim.ClaimStatus> statuses,
            @Param("cutoffDate") LocalDateTime cutoffDate);

    @Query("SELECT c FROM Claim c WHERE " +
            "c.status = :status AND " +
            "c.estimatedAmount > :threshold")
    List<Claim> findHighValueClaimsByStatus(
            @Param("status") Claim.ClaimStatus status,
            @Param("threshold") java.math.BigDecimal threshold);

    @Query(value = "SELECT * FROM CLAIMS c WHERE " +
            "c.status = :status AND " +
            "EXISTS (SELECT 1 FROM CLAIM_DOCUMENTS d WHERE d.CLAIM_ID = c.ID AND d.DOCUMENT_TYPE = :documentType)",
            nativeQuery = true)
    List<Claim> findByStatusAndHavingDocumentType(
            @Param("status") String status,
            @Param("documentType") String documentType);

    @Query("SELECT COUNT(c) FROM Claim c WHERE c.customerId = :customerId")
    long countClaimsByCustomer(@Param("customerId") Long customerId);

    @Query("SELECT c FROM Claim c JOIN c.items i WHERE i.covered = true GROUP BY c HAVING COUNT(i) > 0")
    List<Claim> findClaimsWithCoveredItems();

    @Query("SELECT c FROM Claim c WHERE " +
            "(:claimNumber IS NULL OR c.claimNumber = :claimNumber) AND " +
            "(:policyNumber IS NULL OR c.policyNumber = :policyNumber) AND " +
            "(:customerNumber IS NULL OR c.customerNumber = :customerNumber) AND " +
            "(:status IS NULL OR c.status = :status) AND " +
            "(:claimType IS NULL OR c.claimType = :claimType) AND " +
            "(:incidentDateFrom IS NULL OR c.incidentDate >= :incidentDateFrom) AND " +
            "(:incidentDateTo IS NULL OR c.incidentDate <= :incidentDateTo)")
    List<Claim> findByMultipleParameters(
            @Param("claimNumber") String claimNumber,
            @Param("policyNumber") String policyNumber,
            @Param("customerNumber") String customerNumber,
            @Param("status") Claim.ClaimStatus status,
            @Param("claimType") Claim.ClaimType claimType,
            @Param("incidentDateFrom") LocalDate incidentDateFrom,
            @Param("incidentDateTo") LocalDate incidentDateTo);
}
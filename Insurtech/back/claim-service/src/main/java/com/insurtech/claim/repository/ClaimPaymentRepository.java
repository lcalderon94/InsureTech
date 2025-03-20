package com.insurtech.claim.repository;

import com.insurtech.claim.model.entity.ClaimPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClaimPaymentRepository extends JpaRepository<ClaimPayment, Long> {

    List<ClaimPayment> findByClaimId(Long claimId);

    Optional<ClaimPayment> findByPaymentNumber(String paymentNumber);

    List<ClaimPayment> findByClaimIdAndPaymentStatus(Long claimId, ClaimPayment.PaymentStatus paymentStatus);

    List<ClaimPayment> findByPaymentStatus(ClaimPayment.PaymentStatus paymentStatus);

    @Query("SELECT SUM(p.amount) FROM ClaimPayment p WHERE p.claim.id = :claimId AND p.paymentStatus = 'COMPLETED'")
    BigDecimal sumCompletedPaymentsByClaimId(@Param("claimId") Long claimId);

    @Query("SELECT p FROM ClaimPayment p WHERE p.claim.id = :claimId ORDER BY p.createdAt DESC")
    List<ClaimPayment> findLatestPaymentsByClaimId(@Param("claimId") Long claimId);
}
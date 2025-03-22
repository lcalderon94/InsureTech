package com.insurtech.payment.repository;

import com.insurtech.payment.model.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByPaymentNumber(String paymentNumber);

    List<Payment> findByCustomerNumber(String customerNumber);

    Page<Payment> findByCustomerNumber(String customerNumber, Pageable pageable);

    List<Payment> findByPolicyNumber(String policyNumber);

    Page<Payment> findByPolicyNumber(String policyNumber, Pageable pageable);

    List<Payment> findByStatus(Payment.PaymentStatus status);

    Page<Payment> findByStatus(Payment.PaymentStatus status, Pageable pageable);

    List<Payment> findByCustomerNumberAndStatus(String customerNumber, Payment.PaymentStatus status);

    List<Payment> findByPaymentMethodId(Long paymentMethodId);

    List<Payment> findByPaymentPlanId(Long paymentPlanId);

    List<Payment> findByInvoiceId(Long invoiceId);

    @Query("SELECT p FROM Payment p WHERE p.status = 'PENDING' AND p.dueDate BETWEEN :startDate AND :endDate ORDER BY p.dueDate ASC")
    List<Payment> findPendingPaymentsWithDueDateBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT p FROM Payment p WHERE p.status = 'COMPLETED' AND p.paymentDate BETWEEN :startDate AND :endDate ORDER BY p.paymentDate DESC")
    List<Payment> findCompletedPaymentsWithinDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT p FROM Payment p WHERE " +
            "LOWER(p.paymentNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(p.customerNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(p.policyNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "(p.invoice IS NOT NULL AND LOWER(p.invoice.invoiceNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<Payment> searchPayments(@Param("searchTerm") String searchTerm, Pageable pageable);

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.customerNumber = :customerNumber AND p.paymentType = :paymentType AND p.status = 'COMPLETED'")
    BigDecimal sumCompletedPaymentsByCustomerAndType(
            @Param("customerNumber") String customerNumber,
            @Param("paymentType") Payment.PaymentType paymentType);

    @Query("SELECT COUNT(p) FROM Payment p WHERE p.paymentMethod.id = :paymentMethodId AND p.status = 'FAILED'")
    Long countFailedPaymentsByPaymentMethod(@Param("paymentMethodId") Long paymentMethodId);

    List<Payment> findByExternalId(String externalId);

    List<Payment> findByConcept(String concept);

    @Query("SELECT p FROM Payment p WHERE p.status = :status AND p.retryCount < :maxRetries AND (p.lastRetryDate IS NULL OR p.lastRetryDate < :cutoffTime)")
    Page<Payment> findFailedPaymentsEligibleForRetry(
            @Param("status") Payment.PaymentStatus status,
            @Param("maxRetries") int maxRetries,
            @Param("cutoffTime") LocalDateTime cutoffTime,
            Pageable pageable);
}
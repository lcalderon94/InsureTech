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

/**
 * Repositorio para operaciones de persistencia de pagos
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /**
     * Busca un pago por su número único
     */
    Optional<Payment> findByPaymentNumber(String paymentNumber);

    /**
     * Busca todos los pagos de un cliente específico
     */
    List<Payment> findByCustomerNumber(String customerNumber);

    /**
     * Busca todos los pagos de un cliente específico con paginación
     */
    Page<Payment> findByCustomerNumber(String customerNumber, Pageable pageable);

    /**
     * Busca todos los pagos asociados a una póliza específica
     */
    List<Payment> findByPolicyNumber(String policyNumber);

    /**
     * Busca todos los pagos asociados a una póliza específica con paginación
     */
    Page<Payment> findByPolicyNumber(String policyNumber, Pageable pageable);

    /**
     * Busca todos los pagos con un estado específico
     */
    List<Payment> findByStatus(Payment.PaymentStatus status);

    /**
     * Busca pagos por método de pago
     */
    List<Payment> findByPaymentMethodId(Long paymentMethodId);

    /**
     * Busca pagos por plan de pago
     */
    List<Payment> findByPaymentPlanId(Long paymentPlanId);

    /**
     * Busca pagos por factura
     */
    List<Payment> findByInvoiceId(Long invoiceId);

    /**
     * Busca pagos pendientes con fecha de vencimiento próxima
     */
    @Query("SELECT p FROM Payment p WHERE p.status = 'PENDING' AND p.dueDate BETWEEN :startDate AND :endDate ORDER BY p.dueDate ASC")
    List<Payment> findPendingPaymentsWithDueDateBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Busca pagos realizados en un rango de fechas
     */
    @Query("SELECT p FROM Payment p WHERE p.status = 'COMPLETED' AND p.paymentDate BETWEEN :startDate AND :endDate ORDER BY p.paymentDate DESC")
    List<Payment> findCompletedPaymentsWithinDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Búsqueda por término (número de pago, número de cliente, número de póliza, número de factura)
     */
    @Query("SELECT p FROM Payment p WHERE " +
            "LOWER(p.paymentNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(p.customerNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(p.policyNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "(p.invoice IS NOT NULL AND LOWER(p.invoice.invoiceNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<Payment> searchPayments(@Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * Calcula el total de pagos por cliente y tipo de pago
     */
    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.customerNumber = :customerNumber AND p.paymentType = :paymentType AND p.status = 'COMPLETED'")
    BigDecimal sumCompletedPaymentsByCustomerAndType(
            @Param("customerNumber") String customerNumber,
            @Param("paymentType") Payment.PaymentType paymentType);

    /**
     * Cuenta el número de pagos fallidos por método de pago
     */
    @Query("SELECT COUNT(p) FROM Payment p WHERE p.paymentMethod.id = :paymentMethodId AND p.status = 'FAILED'")
    Long countFailedPaymentsByPaymentMethod(@Param("paymentMethodId") Long paymentMethodId);

    /**
     * Busca pagos por referencia externa
     */
    List<Payment> findByExternalId(String externalId);

    /**
     * Busca pagos por concepto
     */
    List<Payment> findByConcept(String concept);
}
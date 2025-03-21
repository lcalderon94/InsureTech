package com.insurtech.payment.repository;

import com.insurtech.payment.model.entity.Refund;
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
 * Repositorio para operaciones de persistencia de reembolsos
 */
@Repository
public interface RefundRepository extends JpaRepository<Refund, Long> {

    /**
     * Busca un reembolso por su número único
     */
    Optional<Refund> findByRefundNumber(String refundNumber);

    /**
     * Busca todos los reembolsos de un cliente específico
     */
    List<Refund> findByCustomerNumber(String customerNumber);

    /**
     * Busca todos los reembolsos de un cliente específico con paginación
     */
    Page<Refund> findByCustomerNumber(String customerNumber, Pageable pageable);

    /**
     * Busca todos los reembolsos asociados a una póliza específica
     */
    List<Refund> findByPolicyNumber(String policyNumber);

    /**
     * Busca reembolsos por método de pago
     */
    List<Refund> findByPaymentMethodId(Long paymentMethodId);

    /**
     * Busca reembolsos por estado
     */
    List<Refund> findByStatus(Refund.RefundStatus status);

    /**
     * Busca reembolsos por tipo
     */
    List<Refund> findByRefundType(Refund.RefundType refundType);

    /**
     * Busca reembolsos por pago original
     */
    List<Refund> findByOriginalPaymentNumber(String originalPaymentNumber);

    /**
     * Busca reembolsos por referencia externa
     */
    List<Refund> findByExternalReference(String externalReference);

    /**
     * Busca reembolsos solicitados en un rango de fechas
     */
    List<Refund> findByRequestDateBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Busca reembolsos procesados en un rango de fechas
     */
    List<Refund> findByProcessDateBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Búsqueda por término (número de reembolso, número de cliente, número de póliza, número de pago original)
     */
    @Query("SELECT r FROM Refund r WHERE " +
            "LOWER(r.refundNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(r.customerNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(r.policyNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(r.originalPaymentNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<Refund> searchRefunds(@Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * Calcula el total de reembolsos para un cliente
     */
    @Query("SELECT SUM(r.amount) FROM Refund r WHERE r.customerNumber = :customerNumber AND r.status = 'COMPLETED'")
    BigDecimal sumCompletedRefundsByCustomer(@Param("customerNumber") String customerNumber);

    /**
     * Busca reembolsos que están pendientes de aprobación
     */
    @Query("SELECT r FROM Refund r WHERE r.status = 'REQUESTED' AND r.requestDate < :requestedBefore ORDER BY r.requestDate ASC")
    List<Refund> findPendingRefundsOlderThan(@Param("requestedBefore") LocalDateTime requestedBefore);
}
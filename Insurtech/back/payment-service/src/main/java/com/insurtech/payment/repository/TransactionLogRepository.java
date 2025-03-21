package com.insurtech.payment.repository;

import com.insurtech.payment.model.entity.TransactionLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repositorio para operaciones de persistencia de registros de transacciones
 */
@Repository
public interface TransactionLogRepository extends JpaRepository<TransactionLog, Long> {

    /**
     * Busca logs por ID de transacción
     */
    List<TransactionLog> findByTransactionId(String transactionId);

    /**
     * Busca logs por número de pago
     */
    List<TransactionLog> findByPaymentNumber(String paymentNumber);

    /**
     * Busca logs por número de reembolso
     */
    List<TransactionLog> findByRefundNumber(String refundNumber);

    /**
     * Busca logs por tipo
     */
    List<TransactionLog> findByLogType(TransactionLog.LogType logType);

    /**
     * Busca logs por acción
     */
    List<TransactionLog> findByAction(String action);

    /**
     * Busca logs por IP de origen
     */
    List<TransactionLog> findBySourceIp(String sourceIp);

    /**
     * Busca logs creados en un rango de fechas
     */
    List<TransactionLog> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Busca logs por usuario que los creó
     */
    List<TransactionLog> findByCreatedBy(String createdBy);

    /**
     * Busca logs por ID de correlación
     */
    List<TransactionLog> findByCorrelationId(String correlationId);

    /**
     * Busca logs de error que contienen un mensaje específico
     */
    @Query("SELECT tl FROM TransactionLog tl WHERE tl.logType = 'ERROR' AND LOWER(tl.errorMessage) LIKE LOWER(CONCAT('%', :errorPattern, '%'))")
    List<TransactionLog> findErrorLogsContainingMessage(@Param("errorPattern") String errorPattern);

    /**
     * Búsqueda por términos en varios campos
     */
    @Query("SELECT tl FROM TransactionLog tl WHERE " +
            "LOWER(tl.transactionId) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(tl.paymentNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(tl.refundNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(tl.action) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(tl.details) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(tl.errorMessage) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<TransactionLog> searchLogs(@Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * Cuenta el número de logs de error por tipo en un período
     */
    @Query("SELECT COUNT(tl) FROM TransactionLog tl WHERE tl.logType = 'ERROR' AND tl.createdAt BETWEEN :startDate AND :endDate")
    Long countErrorsInPeriod(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Busca logs de seguridad para auditoría
     */
    @Query("SELECT tl FROM TransactionLog tl WHERE tl.logType = 'SECURITY' AND tl.createdAt BETWEEN :startDate AND :endDate")
    List<TransactionLog> findSecurityLogsForAudit(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Busca logs por cambio de estado específico
     */
    @Query("SELECT tl FROM TransactionLog tl WHERE tl.statusBefore = :fromStatus AND tl.statusAfter = :toStatus")
    List<TransactionLog> findByStatusChange(
            @Param("fromStatus") String fromStatus,
            @Param("toStatus") String toStatus);
}
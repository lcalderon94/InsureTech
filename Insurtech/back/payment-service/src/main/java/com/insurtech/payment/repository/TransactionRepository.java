package com.insurtech.payment.repository;

import com.insurtech.payment.model.entity.Transaction;
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
 * Repositorio para operaciones de persistencia de transacciones
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    /**
     * Busca una transacción por su ID único
     */
    Optional<Transaction> findByTransactionId(String transactionId);

    /**
     * Busca transacciones por tipo
     */
    List<Transaction> findByTransactionType(Transaction.TransactionType transactionType);

    /**
     * Busca transacciones por estado
     */
    List<Transaction> findByStatus(Transaction.TransactionStatus status);

    /**
     * Busca transacciones por pago asociado
     */
    List<Transaction> findByPaymentId(Long paymentId);

    /**
     * Busca transacciones por número de pago
     */
    @Query("SELECT t FROM Transaction t JOIN t.payment p WHERE p.paymentNumber = :paymentNumber")
    List<Transaction> findByPaymentNumber(@Param("paymentNumber") String paymentNumber);

    /**
     * Busca transacciones por método de pago
     */
    List<Transaction> findByPaymentMethodId(Long paymentMethodId);

    /**
     * Busca transacciones por referencia de pasarela
     */
    Optional<Transaction> findByGatewayReference(String gatewayReference);

    /**
     * Busca transacciones realizadas en un rango de fechas
     */
    List<Transaction> findByTransactionDateBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Encuentra las 10 transacciones más recientes con el estado especificado
     */
    List<Transaction> findTop10ByStatusOrderByTransactionDateDesc(Transaction.TransactionStatus status);

    /**
     * Cuenta el número de transacciones por estado
     */
    Long countByStatus(Transaction.TransactionStatus status);

    /**
     * Busca transacciones fallidas para reintento
     */
    @Query("SELECT t FROM Transaction t WHERE t.status = 'FAILED' AND t.retryCount < :maxRetries AND t.retryDate <= :currentDateTime")
    List<Transaction> findFailedTransactionsForRetry(
            @Param("maxRetries") Integer maxRetries,
            @Param("currentDateTime") LocalDateTime currentDateTime);

    /**
     * Busca transacciones que requieren conciliación
     */
    @Query("SELECT t FROM Transaction t WHERE t.status IN ('SUCCESSFUL', 'REVERSED') AND t.isReconciled = false AND t.transactionDate < :cutoffDate")
    List<Transaction> findTransactionsForReconciliation(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Búsqueda por términos (ID de transacción, referencia de pasarela, código de autorización)
     */
    @Query("SELECT t FROM Transaction t WHERE " +
            "LOWER(t.transactionId) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(t.gatewayReference) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(t.authorizationCode) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<Transaction> searchTransactions(@Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * Calcula el total procesado por tipo de transacción en un período
     */
    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.transactionType = :type AND t.status = 'SUCCESSFUL' AND t.transactionDate BETWEEN :startDate AND :endDate")
    BigDecimal sumSuccessfulTransactionsByType(
            @Param("type") Transaction.TransactionType type,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Cuenta el número de transacciones fallidas por código de error
     */
    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.status = 'FAILED' AND t.errorCode = :errorCode")
    Long countFailedTransactionsByErrorCode(@Param("errorCode") String errorCode);

    /**
     * Busca transacciones autorizadas pero no capturadas
     */
    @Query("SELECT t FROM Transaction t WHERE t.status = 'AUTHORIZED' AND t.transactionDate < :cutoffDate")
    List<Transaction> findAuthorizedTransactionsToCapture(@Param("cutoffDate") LocalDateTime cutoffDate);
}
package com.insurtech.payment.repository;

import com.insurtech.payment.model.entity.Invoice;
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
 * Repositorio para operaciones de persistencia de facturas
 */
@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    /**
     * Busca una factura por su número único
     */
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    /**
     * Busca todas las facturas de un cliente específico
     */
    List<Invoice> findByCustomerNumber(String customerNumber);

    /**
     * Busca todas las facturas de un cliente específico con paginación
     */
    Page<Invoice> findByCustomerNumber(String customerNumber, Pageable pageable);

    /**
     * Busca todas las facturas asociadas a una póliza específica
     */
    List<Invoice> findByPolicyNumber(String policyNumber);

    /**
     * Busca todas las facturas asociadas a una póliza específica con paginación
     */
    Page<Invoice> findByPolicyNumber(String policyNumber, Pageable pageable);

    /**
     * Busca todas las facturas con un estado específico
     */
    List<Invoice> findByStatus(Invoice.InvoiceStatus status);

    /**
     * Busca facturas por tipo
     */
    List<Invoice> findByInvoiceType(Invoice.InvoiceType invoiceType);

    /**
     * Busca facturas vencidas (due date pasada y no pagadas completamente)
     */
    @Query("SELECT i FROM Invoice i WHERE i.status IN ('PENDING', 'PARTIALLY_PAID') AND i.dueDate < :currentDate")
    List<Invoice> findOverdueInvoices(@Param("currentDate") LocalDateTime currentDate);

    /**
     * Busca facturas próximas a vencer
     */
    @Query("SELECT i FROM Invoice i WHERE i.status IN ('PENDING', 'PARTIALLY_PAID') AND i.dueDate BETWEEN :startDate AND :endDate ORDER BY i.dueDate ASC")
    List<Invoice> findInvoicesDueSoon(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Busca facturas emitidas en un período específico
     */
    List<Invoice> findByIssueDateBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Búsqueda por término (número de factura, número de cliente, número de póliza)
     */
    @Query("SELECT i FROM Invoice i WHERE " +
            "LOWER(i.invoiceNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(i.customerNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(i.policyNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<Invoice> searchInvoices(@Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * Calcula el total pendiente de pago para un cliente
     */
    @Query("SELECT SUM(i.totalAmount - COALESCE(i.paidAmount, 0)) FROM Invoice i WHERE i.customerNumber = :customerNumber AND i.status IN ('PENDING', 'PARTIALLY_PAID', 'OVERDUE')")
    BigDecimal calculateTotalOutstandingForCustomer(@Param("customerNumber") String customerNumber);

    /**
     * Busca facturas por ID electrónico
     */
    Optional<Invoice> findByElectronicInvoiceId(String electronicInvoiceId);

    /**
     * Cuenta el número de facturas vencidas por cliente
     */
    @Query("SELECT COUNT(i) FROM Invoice i WHERE i.customerNumber = :customerNumber AND i.status = 'OVERDUE'")
    Long countOverdueInvoicesByCustomer(@Param("customerNumber") String customerNumber);
}
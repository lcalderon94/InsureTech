package com.insurtech.payment.service;

import com.insurtech.payment.model.dto.InvoiceDto;
import com.insurtech.payment.model.dto.PaymentDto;
import com.insurtech.payment.model.entity.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Servicio para gestionar operaciones relacionadas con facturas
 */
public interface InvoiceService {

    /**
     * Crea una nueva factura
     */
    InvoiceDto createInvoice(InvoiceDto invoiceDto);

    Map<String, Object> generateInvoiceAgingAnalysis();

    CompletableFuture<Integer> sendInvoiceReminders(int daysBeforeDue, boolean includeOverdue);

    /**
     * Busca una factura por su ID
     */
    Optional<InvoiceDto> getInvoiceById(Long id);

    /**
     * Busca una factura por su número
     */
    Optional<InvoiceDto> getInvoiceByNumber(String invoiceNumber);

    /**
     * Busca facturas para un cliente
     */
    List<InvoiceDto> getInvoicesByCustomerNumber(String customerNumber);

    /**
     * Busca facturas para una póliza
     */
    List<InvoiceDto> getInvoicesByPolicyNumber(String policyNumber);

    /**
     * Busca facturas por estado
     */
    List<InvoiceDto> getInvoicesByStatus(Invoice.InvoiceStatus status);

    /**
     * Busca facturas por término
     */
    Page<InvoiceDto> searchInvoices(String searchTerm, Pageable pageable);

    /**
     * Actualiza una factura existente
     */
    InvoiceDto updateInvoice(Long id, InvoiceDto invoiceDto);

    /**
     * Actualiza el estado de una factura
     */
    InvoiceDto updateInvoiceStatus(String invoiceNumber, Invoice.InvoiceStatus status);

    /**
     * Marca una factura como pagada
     */
    InvoiceDto markInvoiceAsPaid(String invoiceNumber, BigDecimal paidAmount, LocalDateTime paymentDate);

    /**
     * Registra un pago parcial en una factura
     */
    InvoiceDto registerPartialPayment(String invoiceNumber, BigDecimal paidAmount, LocalDateTime paymentDate);

    /**
     * Cancela una factura
     */
    InvoiceDto cancelInvoice(String invoiceNumber, String reason);

    /**
     * Obtiene los pagos asociados a una factura
     */
    List<PaymentDto> getPaymentsByInvoiceNumber(String invoiceNumber);

    /**
     * Genera PDF de una factura
     */
    byte[] generateInvoicePdf(String invoiceNumber);

    /**
     * Envía una factura por correo electrónico
     */
    void sendInvoiceByEmail(String invoiceNumber, String email);

    /**
     * Busca facturas vencidas
     */
    List<InvoiceDto> getOverdueInvoices();

    /**
     * Busca facturas próximas a vencer
     */
    List<InvoiceDto> getInvoicesDueSoon(int daysAhead);

    /**
     * Calcula el total pendiente para un cliente
     */
    BigDecimal calculateOutstandingAmountForCustomer(String customerNumber);

    /**
     * Genera un informe de facturas por período
     */
    byte[] generateInvoiceReport(LocalDateTime startDate, LocalDateTime endDate, String format);

    /**
     * Obtiene estadísticas de facturación
     */
    Map<String, Object> getInvoiceStatistics(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Procesa facturas vencidas de forma asíncrona
     */
    CompletableFuture<Integer> processOverdueInvoices();

    /**
     * Sincroniza facturas con un sistema externo
     */
    CompletableFuture<List<InvoiceDto>> syncInvoicesWithExternalSystem(String externalSystemId);
}
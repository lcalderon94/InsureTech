package com.insurtech.payment.service;

import com.insurtech.payment.model.dto.PaymentDto;
import com.insurtech.payment.model.dto.PaymentMethodDto;
import com.insurtech.payment.model.entity.Payment;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Servicio para gestionar operaciones por lotes sobre pagos
 */
public interface PaymentBatchService {

    /**
     * Procesa por lotes una lista de pagos
     */
    CompletableFuture<List<PaymentDto>> processBatch(List<PaymentDto> payments);

    public CompletableFuture<List<PaymentDto>> processPendingPaymentsBatch(PaymentMethodDto paymentMethodDto);

    /**
     * Procesa por lotes pagos desde un archivo CSV
     */
    CompletableFuture<List<PaymentDto>> processPaymentsFromCsv(InputStream inputStream);

    /**
     * Actualiza masivamente el estado de pagos
     */
    CompletableFuture<Integer> batchStatusUpdate(List<String> paymentNumbers, Payment.PaymentStatus status, String reason);

    /**
     * Cancela masivamente pagos
     */
    CompletableFuture<Integer> batchCancelPayments(List<String> paymentNumbers, String reason);

    /**
     * Exporta pagos a un formato específico (CSV, Excel, etc.)
     */
    CompletableFuture<byte[]> exportPayments(
            List<String> paymentNumbers,
            List<Payment.PaymentStatus> statuses,
            LocalDateTime startDate,
            LocalDateTime endDate,
            String format);

    /**
     * Reconcilia masivamente pagos con transacciones externas
     */
    CompletableFuture<Map<String, Object>> batchReconciliation(LocalDateTime cutoffDate);

    /**
     * Calcula estadísticas sobre un conjunto de pagos
     */
    CompletableFuture<Map<String, Object>> calculateBatchStatistics(
            List<String> paymentNumbers,
            List<Payment.PaymentStatus> statuses,
            LocalDateTime startDate,
            LocalDateTime endDate);

    /**
     * Procesa pagos automáticos pendientes
     */
    CompletableFuture<Integer> processAutoPayments();

    /**
     * Genera informes de rendimiento de pagos
     */
    CompletableFuture<Map<String, Object>> generatePerformanceReport(
            LocalDateTime startDate,
            LocalDateTime endDate);

    /**
     * Actualiza los estados de pagos vencidos
     */
    CompletableFuture<Integer> updateOverduePayments();

    /**
     * Notifica sobre pagos pendientes
     */
    CompletableFuture<Integer> notifyPendingPayments(int daysAhead);

    /**
     * Obtiene el estado de un trabajo por lotes
     */
    Map<String, Object> getBatchStatus(String batchId);

    /**
     * Genera masivamente facturas para pagos
     */
    CompletableFuture<Integer> generateInvoicesForPayments(List<String> paymentNumbers);
}
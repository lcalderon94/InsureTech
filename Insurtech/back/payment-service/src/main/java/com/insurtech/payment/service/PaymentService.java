package com.insurtech.payment.service;

import com.insurtech.payment.model.dto.PaymentDto;
import com.insurtech.payment.model.dto.PaymentRequestDto;
import com.insurtech.payment.model.dto.PaymentResponseDto;
import com.insurtech.payment.model.dto.TransactionDto;
import com.insurtech.payment.model.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Servicio para gestionar operaciones relacionadas con pagos
 */
public interface PaymentService {

    /**
     * Crea un nuevo pago
     */
    PaymentDto createPayment(PaymentDto paymentDto);

    /**
     * Procesa un pago nuevo (crea y procesa en un solo paso)
     */
    PaymentResponseDto processPayment(PaymentRequestDto paymentRequestDto);

    /**
     * Procesa un pago de forma asíncrona
     */
    CompletableFuture<PaymentResponseDto> processPaymentAsync(PaymentRequestDto paymentRequestDto);

    /**
     * Busca un pago por su ID
     */
    Optional<PaymentDto> getPaymentById(Long id);

    /**
     * Busca un pago por su número
     */
    Optional<PaymentDto> getPaymentByNumber(String paymentNumber);

    /**
     * Busca pagos para un cliente
     */
    List<PaymentDto> getPaymentsByCustomerNumber(String customerNumber);

    /**
     * Busca pagos para una póliza
     */
    List<PaymentDto> getPaymentsByPolicyNumber(String policyNumber);

    /**
     * Busca pagos por término
     */
    Page<PaymentDto> searchPayments(String searchTerm, Pageable pageable);

    /**
     * Busca pagos por estado
     */
    List<PaymentDto> getPaymentsByStatus(Payment.PaymentStatus status);

    /**
     * Actualiza un pago existente
     */
    PaymentDto updatePayment(Long id, PaymentDto paymentDto);

    /**
     * Actualiza el estado de un pago
     */
    PaymentDto updatePaymentStatus(String paymentNumber, Payment.PaymentStatus status, String reason);

    /**
     * Cancela un pago pendiente
     */
    PaymentDto cancelPayment(String paymentNumber, String reason);

    /**
     * Obtiene las transacciones de un pago
     */
    List<TransactionDto> getTransactionsByPaymentNumber(String paymentNumber);

    /**
     * Crea una nueva transacción para un pago
     */
    TransactionDto createTransaction(String paymentNumber, TransactionDto transactionDto);

    /**
     * Calcula el total pagado para una póliza
     */
    BigDecimal calculateTotalPaidForPolicy(String policyNumber);

    /**
     * Verifica si un pago está completado
     */
    boolean isPaymentCompleted(String paymentNumber);

    /**
     * Obtiene estadísticas de pagos para un cliente
     */
    Map<String, Object> getPaymentStatistics(String customerNumber);

    /**
     * Obtiene estadísticas de pagos para un periodo
     */
    Map<String, Object> getPaymentStatisticsForPeriod(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Busca pagos pendientes por vencer
     */
    List<PaymentDto> getPendingPaymentsDueSoon(int daysAhead);

    /**
     * Reconcilia pagos con transacciones externas
     */
    CompletableFuture<Integer> reconcilePayments(List<String> externalReferences);

    /**
     * Genera un informe de pagos por período
     */
    byte[] generatePaymentReport(LocalDateTime startDate, LocalDateTime endDate, String format);

    /**
     * Actualiza múltiples pagos en batch
     */
    CompletableFuture<Integer> updatePaymentsInBatch(List<String> paymentNumbers, Payment.PaymentStatus status);
}
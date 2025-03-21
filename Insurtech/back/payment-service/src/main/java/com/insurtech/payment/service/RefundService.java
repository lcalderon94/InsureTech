package com.insurtech.payment.service;

import com.insurtech.payment.model.dto.RefundDto;
import com.insurtech.payment.model.dto.TransactionDto;
import com.insurtech.payment.model.entity.Refund;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Servicio para gestionar operaciones relacionadas con reembolsos
 */
public interface RefundService {

    /**
     * Solicita un nuevo reembolso
     */
    RefundDto requestRefund(RefundDto refundDto);

    /**
     * Procesa un reembolso pendiente
     */
    RefundDto processRefund(String refundNumber);

    /**
     * Procesa un reembolso de forma asíncrona
     */
    CompletableFuture<RefundDto> processRefundAsync(String refundNumber);

    /**
     * Busca un reembolso por su ID
     */
    Optional<RefundDto> getRefundById(Long id);

    /**
     * Busca un reembolso por su número
     */
    Optional<RefundDto> getRefundByNumber(String refundNumber);

    /**
     * Busca reembolsos para un cliente
     */
    List<RefundDto> getRefundsByCustomerNumber(String customerNumber);

    /**
     * Busca reembolsos para una póliza
     */
    List<RefundDto> getRefundsByPolicyNumber(String policyNumber);

    /**
     * Busca reembolsos por estado
     */
    List<RefundDto> getRefundsByStatus(Refund.RefundStatus status);

    /**
     * Busca reembolsos por término
     */
    Page<RefundDto> searchRefunds(String searchTerm, Pageable pageable);

    /**
     * Actualiza un reembolso existente
     */
    RefundDto updateRefund(Long id, RefundDto refundDto);

    /**
     * Actualiza el estado de un reembolso
     */
    RefundDto updateRefundStatus(String refundNumber, Refund.RefundStatus status, String reason);

    /**
     * Aprueba un reembolso solicitado
     */
    RefundDto approveRefund(String refundNumber);

    /**
     * Rechaza un reembolso solicitado
     */
    RefundDto rejectRefund(String refundNumber, String reason);

    /**
     * Completa manualmente un reembolso
     */
    RefundDto completeRefund(String refundNumber, String externalReference);

    /**
     * Cancela un reembolso pendiente
     */
    RefundDto cancelRefund(String refundNumber, String reason);

    /**
     * Obtiene la transacción asociada a un reembolso
     */
    Optional<TransactionDto> getRefundTransaction(String refundNumber);

    /**
     * Calcula el total reembolsado a un cliente
     */
    BigDecimal calculateTotalRefundedForCustomer(String customerNumber);

    /**
     * Calcula el total reembolsado para una póliza
     */
    BigDecimal calculateTotalRefundedForPolicy(String policyNumber);

    /**
     * Obtiene estadísticas de reembolsos
     */
    Map<String, Object> getRefundStatistics(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Genera un informe de reembolsos
     */
    byte[] generateRefundReport(LocalDateTime startDate, LocalDateTime endDate, String format);

    /**
     * Procesa reembolsos pendientes de forma asíncrona
     */
    CompletableFuture<Integer> processPendingRefunds();

    /**
     * Notifica sobre reembolsos procesados
     */
    CompletableFuture<Integer> notifyProcessedRefunds();
}
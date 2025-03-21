package com.insurtech.payment.service;

import com.insurtech.payment.model.dto.PaymentMethodDto;
import com.insurtech.payment.model.dto.TransactionDto;
import com.insurtech.payment.model.entity.Payment;
import com.insurtech.payment.model.entity.Transaction;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Servicio para integración con pasarelas de pago externas
 */
public interface PaymentGatewayService {

    /**
     * Procesa una transacción de pago
     *
     * @param amount Monto a procesar
     * @param currency Moneda
     * @param paymentMethodDto Método de pago a utilizar
     * @param description Descripción de la transacción
     * @param metadata Metadatos adicionales
     * @return La transacción procesada
     */
    TransactionDto processPaymentTransaction(
            BigDecimal amount,
            String currency,
            PaymentMethodDto paymentMethodDto,
            String description,
            Map<String, String> metadata);

    /**
     * Procesa una transacción de pago de forma asíncrona
     */
    CompletableFuture<TransactionDto> processPaymentTransactionAsync(
            BigDecimal amount,
            String currency,
            PaymentMethodDto paymentMethodDto,
            String description,
            Map<String, String> metadata);

    /**
     * Autoriza una transacción sin capturarla
     */
    TransactionDto authorizeTransaction(
            BigDecimal amount,
            String currency,
            PaymentMethodDto paymentMethodDto,
            String description,
            Map<String, String> metadata);

    /**
     * Captura una transacción previamente autorizada
     */
    TransactionDto captureTransaction(String authorizationId, BigDecimal amount);

    /**
     * Procesa un reembolso
     */
    TransactionDto processRefund(
            String originalTransactionId,
            BigDecimal amount,
            String currency,
            String reason,
            Map<String, String> metadata);

    /**
     * Anula una transacción
     */
    TransactionDto voidTransaction(String transactionId, String reason);

    /**
     * Verifica el estado de una transacción
     */
    Transaction.TransactionStatus checkTransactionStatus(String transactionId);

    /**
     * Tokeniza un método de pago para uso futuro
     */
    String tokenizePaymentMethod(PaymentMethodDto paymentMethodDto);

    /**
     * Crea un cliente en la pasarela de pago
     */
    String createCustomerProfile(String customerNumber, String email, String name);

    /**
     * Asocia un método de pago a un cliente
     */
    String attachPaymentMethodToCustomer(String customerProfileId, PaymentMethodDto paymentMethodDto);

    /**
     * Configura pagos recurrentes
     */
    String setupRecurringPayment(
            String customerProfileId,
            String paymentMethodToken,
            BigDecimal amount,
            String currency,
            String frequency,
            int totalPayments,
            String description);

    /**
     * Cancela un plan de pagos recurrentes
     */
    boolean cancelRecurringPayment(String recurringPaymentId, String reason);

    /**
     * Verifica si un método de pago es válido
     */
    boolean validatePaymentMethod(PaymentMethodDto paymentMethodDto);

    /**
     * Obtiene datos detallados de una transacción
     */
    Map<String, Object> getTransactionDetails(String transactionId);
}
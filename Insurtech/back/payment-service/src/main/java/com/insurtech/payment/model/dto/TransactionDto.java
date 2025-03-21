package com.insurtech.payment.model.dto;

import com.insurtech.payment.model.entity.Transaction;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO para transferencia de datos de transacciones
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDto {

    private Long id;

    private String transactionId;

    @NotNull(message = "El tipo de transacción es obligatorio")
    private Transaction.TransactionType transactionType;

    @NotNull(message = "El monto es obligatorio")
    @Positive(message = "El monto debe ser mayor que cero")
    private BigDecimal amount;

    @NotBlank(message = "La moneda es obligatoria")
    private String currency;

    private Transaction.TransactionStatus status;

    private LocalDateTime transactionDate;

    private String gatewayReference;

    private String gatewayResponseCode;

    private String gatewayResponseMessage;

    private String authorizationCode;

    private String errorCode;

    private String errorDescription;

    private Integer retryCount;

    private LocalDateTime retryDate;

    private boolean isReconciled;

    private LocalDateTime reconciliationDate;

    // Información del pago asociado
    private Long paymentId;
    private String paymentNumber;

    // Información del reembolso asociado
    private Long refundId;
    private String refundNumber;

    // Información del método de pago
    private Long paymentMethodId;
    private String paymentMethodNumber;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
package com.insurtech.payment.model.dto;

import com.insurtech.payment.model.entity.Refund;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO para transferencia de datos de reembolsos
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefundDto {

    private Long id;

    private String refundNumber;

    @NotBlank(message = "El número de cliente es obligatorio")
    private String customerNumber;

    private String policyNumber;

    private String originalPaymentNumber;

    @NotNull(message = "El tipo de reembolso es obligatorio")
    private Refund.RefundType refundType;

    @NotNull(message = "El monto es obligatorio")
    @Positive(message = "El monto debe ser mayor que cero")
    private BigDecimal amount;

    @NotBlank(message = "La moneda es obligatoria")
    private String currency;

    private Refund.RefundStatus status;

    private LocalDateTime requestDate;

    private LocalDateTime processDate;

    @NotBlank(message = "El motivo del reembolso es obligatorio")
    private String reason;

    private String description;

    private String externalReference;

    // Información del método de pago
    private Long paymentMethodId;
    private String paymentMethodNumber;
    private PaymentMethodDto paymentMethod;

    // Información de la transacción
    private Long transactionId;
    private String transactionReference;
    private TransactionDto transaction;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
package com.insurtech.payment.model.dto;

import com.insurtech.payment.model.entity.Payment;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * DTO para transferencia de datos de pagos
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDto {

    private Long id;

    private String paymentNumber;

    private String policyNumber;

    @NotBlank(message = "El número de cliente es obligatorio")
    private String customerNumber;

    @NotNull(message = "El tipo de pago es obligatorio")
    private Payment.PaymentType paymentType;

    @NotBlank(message = "El concepto de pago es obligatorio")
    private String concept;

    @NotNull(message = "El monto es obligatorio")
    @Positive(message = "El monto debe ser mayor que cero")
    private BigDecimal amount;

    @NotBlank(message = "La moneda es obligatoria")
    private String currency;

    private Payment.PaymentStatus status;

    private LocalDateTime dueDate;

    private LocalDateTime paymentDate;

    private String reference;

    private String externalId;

    private String description;

    // Información del método de pago
    private Long paymentMethodId;
    private String paymentMethodNumber;
    private PaymentMethodDto paymentMethod;

    // Información del plan de pago
    private Long paymentPlanId;
    private String paymentPlanNumber;

    // Información de factura
    private Long invoiceId;
    private String invoiceNumber;

    // Información de transacciones
    private Set<TransactionDto> transactions;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
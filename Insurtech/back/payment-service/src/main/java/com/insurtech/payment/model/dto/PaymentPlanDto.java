package com.insurtech.payment.model.dto;

import com.insurtech.payment.model.entity.PaymentPlan;
import jakarta.validation.constraints.Min;
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
 * DTO para transferencia de datos de planes de pago
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentPlanDto {

    private Long id;

    private String paymentPlanNumber;

    @NotBlank(message = "El número de póliza es obligatorio")
    private String policyNumber;

    @NotBlank(message = "El número de cliente es obligatorio")
    private String customerNumber;

    @NotNull(message = "El tipo de plan de pago es obligatorio")
    private PaymentPlan.PlanType planType;

    @NotNull(message = "La frecuencia es obligatoria")
    private PaymentPlan.Frequency frequency;

    @NotNull(message = "El número de cuotas es obligatorio")
    @Min(value = 1, message = "El número de cuotas debe ser al menos 1")
    private Integer installments;

    @NotNull(message = "El monto total es obligatorio")
    @Positive(message = "El monto total debe ser mayor que cero")
    private BigDecimal totalAmount;

    @NotNull(message = "El monto de la cuota es obligatorio")
    @Positive(message = "El monto de la cuota debe ser mayor que cero")
    private BigDecimal installmentAmount;

    @NotNull(message = "La fecha del primer pago es obligatoria")
    private LocalDateTime firstPaymentDate;

    private LocalDateTime lastPaymentDate;

    @NotBlank(message = "La moneda es obligatoria")
    private String currency;

    private PaymentPlan.PlanStatus status;

    private boolean isAutoPayment;

    private Integer paymentDay;

    private String description;

    // Información del método de pago
    private Long paymentMethodId;
    private String paymentMethodNumber;
    private PaymentMethodDto paymentMethod;

    // Pagos asociados
    private Set<PaymentDto> payments;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
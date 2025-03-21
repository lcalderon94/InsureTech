package com.insurtech.payment.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO para solicitudes de procesamiento de pago
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequestDto {

    @NotBlank(message = "El número de cliente es obligatorio")
    private String customerNumber;

    private String policyNumber;

    private String invoiceNumber;

    @NotNull(message = "El monto es obligatorio")
    @Positive(message = "El monto debe ser mayor que cero")
    private BigDecimal amount;

    @NotBlank(message = "La moneda es obligatoria")
    private String currency;

    @NotBlank(message = "El concepto de pago es obligatorio")
    private String concept;

    private String description;

    // Información del método de pago
    private Long paymentMethodId;
    private String paymentMethodNumber;

    // Datos de tarjeta para pagos únicos (no se guardan)
    private String cardHolderName;
    private String cardNumber;
    private String cardExpiryMonth;
    private String cardExpiryYear;
    private String cvv;

    // Información para pagos recurrentes
    private boolean setupRecurring;
    private String recurringFrequency;
    private Integer recurringInstallments;
    private LocalDateTime firstPaymentDate;
    private boolean savePaymentMethod;

    // Datos de dirección de facturación
    private String billingAddress;
    private String billingCity;
    private String billingState;
    private String billingZip;
    private String billingCountry;

    // Datos de correo electrónico para notificaciones
    private String email;
    private boolean sendReceipt;
}
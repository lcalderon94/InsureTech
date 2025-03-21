package com.insurtech.payment.model.dto;

import com.insurtech.payment.model.entity.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.YearMonth;

/**
 * DTO para transferencia de datos de métodos de pago
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMethodDto {

    private Long id;

    private String paymentMethodNumber;

    @NotBlank(message = "El número de cliente es obligatorio")
    private String customerNumber;

    @NotNull(message = "El tipo de método de pago es obligatorio")
    private PaymentMethod.MethodType methodType;

    @NotBlank(message = "El nombre del método de pago es obligatorio")
    private String name;

    private boolean isDefault;

    private boolean isActive = true;

    private boolean isVerified;

    // Campos específicos según el tipo de método de pago
    // Tarjeta de crédito/débito
    private String cardHolderName;
    private String maskedCardNumber;
    private String cardType;
    private YearMonth cardExpiryDate;

    // Cuenta bancaria
    private String bankName;
    private String accountNumber;
    private String accountHolderName;
    private String accountType;

    // Monedero electrónico
    private String walletProvider;
    private String walletId;

    // Parámetros seguros para la creación/actualización (no se devuelven en respuestas)
    private String fullCardNumber;
    private String cvv;
    private String routingNumber;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
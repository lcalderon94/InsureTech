package com.insurtech.payment.model.dto; /**
 * DTO para respuestas de procesamiento de pago
 */

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponseDto {

    private String paymentNumber;
    private String transactionId;
    private String status;
    private boolean successful;
    private BigDecimal amount;
    private String currency;
    private LocalDateTime processingDate;
    private String authorizationCode;
    private String receiptUrl;
    private String errorCode;
    private String errorMessage;

    // Información de pago recurrente (si aplica)
    private String paymentPlanNumber;
    private Integer installmentsTotal;
    private Integer installmentsProcessed;
    private LocalDateTime nextPaymentDate;

    // Información del método de pago utilizado
    private String paymentMethodNumber;
    private String maskedCardNumber;
    private String cardType;
}
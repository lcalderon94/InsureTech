package com.insurtech.payment.model.dto;

import com.insurtech.payment.model.entity.Invoice;
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
 * DTO para transferencia de datos de facturas
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceDto {

    private Long id;

    private String invoiceNumber;

    private String policyNumber;

    @NotBlank(message = "El número de cliente es obligatorio")
    private String customerNumber;

    @NotNull(message = "El tipo de factura es obligatorio")
    private Invoice.InvoiceType invoiceType;

    @NotNull(message = "La fecha de emisión es obligatoria")
    private LocalDateTime issueDate;

    @NotNull(message = "La fecha de vencimiento es obligatoria")
    private LocalDateTime dueDate;

    @NotNull(message = "El monto total es obligatorio")
    @Positive(message = "El monto total debe ser mayor que cero")
    private BigDecimal totalAmount;

    private BigDecimal taxAmount;

    private BigDecimal netAmount;

    @NotBlank(message = "La moneda es obligatoria")
    private String currency;

    private Invoice.InvoiceStatus status;

    private BigDecimal paidAmount;

    private LocalDateTime paymentDate;

    private String description;

    private String electronicInvoiceId;

    // Información de pagos asociados
    private Set<PaymentDto> payments;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
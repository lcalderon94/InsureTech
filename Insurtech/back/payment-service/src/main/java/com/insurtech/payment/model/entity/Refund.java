package com.insurtech.payment.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "REFUNDS")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Refund {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_REFUNDS")
    @SequenceGenerator(name = "SEQ_REFUNDS", sequenceName = "SEQ_REFUNDS", allocationSize = 1)
    private Long id;

    @Column(name = "REFUND_NUMBER", unique = true, nullable = false)
    private String refundNumber;

    @Column(name = "CUSTOMER_NUMBER", nullable = false)
    private String customerNumber;

    @Column(name = "POLICY_NUMBER")
    private String policyNumber;

    @Column(name = "ORIGINAL_PAYMENT_NUMBER")
    private String originalPaymentNumber;

    @Column(name = "REFUND_TYPE", nullable = false)
    @Enumerated(EnumType.STRING)
    private RefundType refundType;

    @Column(name = "AMOUNT", precision = 19, scale = 4, nullable = false)
    private BigDecimal amount;

    @Column(name = "CURRENCY", length = 3, nullable = false)
    private String currency;

    @Column(name = "REFUND_STATUS", nullable = false)
    @Enumerated(EnumType.STRING)
    private RefundStatus status;

    @Column(name = "REQUEST_DATE", nullable = false)
    private LocalDateTime requestDate;

    @Column(name = "PROCESS_DATE")
    private LocalDateTime processDate;

    @Column(name = "REFUND_REASON", nullable = false)
    private String reason;

    // Se indica que este campo es un LOB para mapear el CLOB de la BD
    @Lob
    @Column(name = "REFUND_DESCRIPTION")
    private String description;

    @Column(name = "EXTERNAL_REFERENCE")
    private String externalReference;

    // Relaciones
    @ManyToOne
    @JoinColumn(name = "PAYMENT_METHOD_ID")
    private PaymentMethod paymentMethod;

    @OneToOne
    @JoinColumn(name = "TRANSACTION_ID")
    private Transaction transaction;

    // Campos de auditoría
    @CreationTimestamp
    @Column(name = "CREATED_AT", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    @Column(name = "CREATED_BY")
    private String createdBy;

    @Column(name = "UPDATED_BY")
    private String updatedBy;

    @Version
    private Long version;

    // Enumeraciones
    public enum RefundType {
        CANCELLATION,        // Reembolso por cancelación de póliza
        OVERPAYMENT,         // Reembolso por pago en exceso
        DUPLICATE_PAYMENT,   // Reembolso por pago duplicado
        POLICY_ADJUSTMENT,   // Reembolso por ajuste de póliza
        CLAIM_ADJUSTMENT,    // Reembolso por ajuste de reclamación
        CUSTOMER_REQUEST     // Reembolso por solicitud del cliente
    }

    public enum RefundStatus {
        REQUESTED,    // Solicitado
        APPROVED,     // Aprobado
        REJECTED,     // Rechazado
        PROCESSING,   // En procesamiento
        COMPLETED,    // Completado
        FAILED        // Fallido
    }
}

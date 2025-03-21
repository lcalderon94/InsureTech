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

/**
 * Entidad para gestionar las transacciones individuales de pago/reembolso
 * Una transacción representa un intento concreto de procesar un pago o reembolso
 */
@Entity
@Table(name = "TRANSACTIONS")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Transaction {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_TRANSACTIONS")
    @SequenceGenerator(name = "SEQ_TRANSACTIONS", sequenceName = "SEQ_TRANSACTIONS", allocationSize = 1)
    private Long id;

    @Column(name = "TRANSACTION_ID", unique = true, nullable = false)
    private String transactionId;

    @Column(name = "TRANSACTION_TYPE", nullable = false)
    @Enumerated(EnumType.STRING)
    private TransactionType transactionType;

    @Column(name = "AMOUNT", precision = 19, scale = 4, nullable = false)
    private BigDecimal amount;

    @Column(name = "CURRENCY", length = 3, nullable = false)
    private String currency;

    @Column(name = "TRANSACTION_STATUS", nullable = false)
    @Enumerated(EnumType.STRING)
    private TransactionStatus status;

    @Column(name = "TRANSACTION_DATE", nullable = false)
    private LocalDateTime transactionDate;

    @Column(name = "GATEWAY_REFERENCE")
    private String gatewayReference;

    @Column(name = "GATEWAY_RESPONSE_CODE")
    private String gatewayResponseCode;

    @Column(name = "GATEWAY_RESPONSE_MESSAGE")
    private String gatewayResponseMessage;

    @Column(name = "AUTHORIZATION_CODE")
    private String authorizationCode;

    @Column(name = "ERROR_CODE")
    private String errorCode;

    @Column(name = "ERROR_DESCRIPTION")
    private String errorDescription;

    @Column(name = "RETRY_COUNT")
    private Integer retryCount = 0;

    @Column(name = "RETRY_DATE")
    private LocalDateTime retryDate;

    @Column(name = "IS_RECONCILED")
    private boolean isReconciled = false;

    @Column(name = "RECONCILIATION_DATE")
    private LocalDateTime reconciliationDate;

    // Relaciones
    @ManyToOne
    @JoinColumn(name = "PAYMENT_ID")
    private Payment payment;

    @OneToOne(mappedBy = "transaction")
    private Refund refund;

    @ManyToOne
    @JoinColumn(name = "PAYMENT_METHOD_ID")
    private PaymentMethod paymentMethod;

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
    public enum TransactionType {
        PAYMENT,    // Transacción de pago
        REFUND,     // Transacción de reembolso
        REVERSAL,   // Reversión de transacción
        ADJUSTMENT, // Ajuste manual
        AUTH_ONLY,  // Solo autorización
        CAPTURE,    // Captura de autorización previa
        VOID        // Anulación
    }

    public enum TransactionStatus {
        PENDING,        // Pendiente
        PROCESSING,     // En procesamiento
        AUTHORIZED,     // Autorizada pero no capturada
        SUCCESSFUL,     // Exitosa
        FAILED,         // Fallida
        REJECTED,       // Rechazada por gateway
        CANCELLED,      // Cancelada
        PENDING_REVIEW, // Pendiente de revisión manual
        REVERSED        // Reversada
    }
}
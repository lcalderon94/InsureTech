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
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "PAYMENTS")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Payment {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_PAYMENTS")
    @SequenceGenerator(name = "SEQ_PAYMENTS", sequenceName = "SEQ_PAYMENTS", allocationSize = 1)
    private Long id;

    @Column(name = "PAYMENT_NUMBER", unique = true, nullable = false)
    private String paymentNumber;

    @Column(name = "POLICY_NUMBER")
    private String policyNumber;

    @Column(name = "CUSTOMER_NUMBER", nullable = false)
    private String customerNumber;

    @Column(name = "PAYMENT_TYPE", nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentType paymentType;

    @Column(name = "PAYMENT_CONCEPT", nullable = false)
    private String concept;

    @Column(name = "AMOUNT", precision = 19, scale = 4, nullable = false)
    private BigDecimal amount;

    @Column(name = "CURRENCY", length = 3, nullable = false)
    private String currency;

    @Column(name = "PAYMENT_STATUS", nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    @Column(name = "DUE_DATE")
    private LocalDateTime dueDate;

    @Column(name = "PAYMENT_DATE")
    private LocalDateTime paymentDate;

    @Column(name = "REFERENCE")
    private String reference;

    @Column(name = "EXTERNAL_ID")
    private String externalId;

    // Se indica que este campo es un LOB para mapear el CLOB de la BD
    @Lob
    @Column(name = "PAYMENT_DESCRIPTION")
    private String description;

    @Column(name = "FAILURE_REASON")
    private String failureReason;

    @Column(name = "RETRY_COUNT")
    private Integer retryCount = 0;

    @Column(name = "LAST_RETRY_DATE")
    private LocalDateTime lastRetryDate;

    @Column(name = "COMPLETION_DATE")
    private LocalDateTime completionDate;

    // Relaciones
    @ManyToOne
    @JoinColumn(name = "PAYMENT_METHOD_ID")
    private PaymentMethod paymentMethod;

    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Transaction> transactions = new HashSet<>();

    @ManyToOne
    @JoinColumn(name = "PAYMENT_PLAN_ID")
    private PaymentPlan paymentPlan;

    @ManyToOne
    @JoinColumn(name = "INVOICE_ID")
    private Invoice invoice;

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
    public enum PaymentType {
        PREMIUM, // Pago de prima de seguro
        CLAIM,   // Pago de reclamación
        REFUND,  // Reembolso
        FEE,     // Cargo por servicio
        TAX      // Impuesto
    }

    public enum PaymentStatus {
        PENDING,    // Pendiente de pago
        PROCESSING, // En procesamiento
        COMPLETED,  // Completado con éxito
        FAILED,     // Fallido
        CANCELLED,  // Cancelado
        REFUNDED,   // Reembolsado
        EXPIRED     // Expirado por tiempo
    }

    // Métodos para gestionar relaciones bidireccionales
    public void addTransaction(Transaction transaction) {
        transactions.add(transaction);
        transaction.setPayment(this);
    }

    public void removeTransaction(Transaction transaction) {
        transactions.remove(transaction);
        transaction.setPayment(null);
    }
}

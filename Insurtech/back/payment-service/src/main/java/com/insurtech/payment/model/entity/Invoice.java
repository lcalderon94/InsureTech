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

/**
 * Entidad para gestionar las facturas en el sistema
 */
@Entity
@Table(name = "INVOICES")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Invoice {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_INVOICES")
    @SequenceGenerator(name = "SEQ_INVOICES", sequenceName = "SEQ_INVOICES", allocationSize = 1)
    private Long id;

    @Column(name = "INVOICE_NUMBER", unique = true, nullable = false)
    private String invoiceNumber;

    @Column(name = "POLICY_NUMBER")
    private String policyNumber;

    @Column(name = "CUSTOMER_NUMBER", nullable = false)
    private String customerNumber;

    @Column(name = "INVOICE_TYPE", nullable = false)
    @Enumerated(EnumType.STRING)
    private InvoiceType invoiceType;

    @Column(name = "ISSUE_DATE", nullable = false)
    private LocalDateTime issueDate;

    @Column(name = "DUE_DATE", nullable = false)
    private LocalDateTime dueDate;

    @Column(name = "TOTAL_AMOUNT", precision = 19, scale = 4, nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "TAX_AMOUNT", precision = 19, scale = 4)
    private BigDecimal taxAmount;

    @Column(name = "NET_AMOUNT", precision = 19, scale = 4)
    private BigDecimal netAmount;

    @Column(name = "CURRENCY", length = 3, nullable = false)
    private String currency;

    @Column(name = "INVOICE_STATUS", nullable = false)
    @Enumerated(EnumType.STRING)
    private InvoiceStatus status;

    @Column(name = "PAID_AMOUNT", precision = 19, scale = 4)
    private BigDecimal paidAmount;

    @Column(name = "PAYMENT_DATE")
    private LocalDateTime paymentDate;

    @Column(name = "INVOICE_DESCRIPTION")
    private String description;

    @Column(name = "ELECTRONIC_INVOICE_ID")
    private String electronicInvoiceId;

    // Relaciones
    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Payment> payments = new HashSet<>();

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
    public enum InvoiceType {
        PREMIUM,    // Factura de prima de seguro
        SERVICE,    // Factura por servicios
        ADJUSTMENT, // Ajuste
        CREDIT_NOTE // Nota de crédito
    }

    public enum InvoiceStatus {
        DRAFT,     // Borrador
        ISSUED,    // Emitida
        PENDING,   // Pendiente de pago
        PARTIALLY_PAID, // Parcialmente pagada
        PAID,      // Pagada completamente
        CANCELLED, // Cancelada
        OVERDUE    // Vencida
    }

    // Métodos para gestionar relaciones bidireccionales
    public void addPayment(Payment payment) {
        payments.add(payment);
        payment.setInvoice(this);
    }

    public void removePayment(Payment payment) {
        payments.remove(payment);
        payment.setInvoice(null);
    }
}
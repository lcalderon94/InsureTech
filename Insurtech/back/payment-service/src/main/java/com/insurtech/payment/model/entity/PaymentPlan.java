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
 * Entidad para gestionar los planes de pago en el sistema
 * Un plan de pago define la forma en que el cliente realizará los pagos de una póliza
 */
@Entity
@Table(name = "PAYMENT_PLANS")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class PaymentPlan {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_PAYMENT_PLANS")
    @SequenceGenerator(name = "SEQ_PAYMENT_PLANS", sequenceName = "SEQ_PAYMENT_PLANS", allocationSize = 1)
    private Long id;

    @Column(name = "PAYMENT_PLAN_NUMBER", unique = true, nullable = false)
    private String paymentPlanNumber;

    @Column(name = "POLICY_NUMBER", nullable = false)
    private String policyNumber;

    @Column(name = "CUSTOMER_NUMBER", nullable = false)
    private String customerNumber;

    @Column(name = "PAYMENT_PLAN_TYPE", nullable = false)
    @Enumerated(EnumType.STRING)
    private PlanType planType;

    @Column(name = "FREQUENCY", nullable = false)
    @Enumerated(EnumType.STRING)
    private Frequency frequency;

    @Column(name = "INSTALLMENTS", nullable = false)
    private Integer installments;

    @Column(name = "TOTAL_AMOUNT", precision = 19, scale = 4, nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "INSTALLMENT_AMOUNT", precision = 19, scale = 4, nullable = false)
    private BigDecimal installmentAmount;

    @Column(name = "FIRST_PAYMENT_DATE", nullable = false)
    private LocalDateTime firstPaymentDate;

    @Column(name = "LAST_PAYMENT_DATE")
    private LocalDateTime lastPaymentDate;

    @Column(name = "CURRENCY", length = 3, nullable = false)
    private String currency;

    @Column(name = "PAYMENT_PLAN_STATUS", nullable = false)
    @Enumerated(EnumType.STRING)
    private PlanStatus status;

    @Column(name = "IS_AUTO_PAYMENT")
    private boolean isAutoPayment;

    @Column(name = "PAYMENT_DAY")
    private Integer paymentDay;

    @Column(name = "DESCRIPTION")
    private String description;

    // Relaciones
    @ManyToOne
    @JoinColumn(name = "PAYMENT_METHOD_ID")
    private PaymentMethod paymentMethod;

    @OneToMany(mappedBy = "paymentPlan", cascade = CascadeType.ALL, orphanRemoval = true)
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
    public enum PlanType {
        FULL_PAYMENT,      // Pago único
        INSTALLMENTS,      // Pago en cuotas
        RECURRING,         // Pagos recurrentes
        CUSTOM             // Plan personalizado
    }

    public enum Frequency {
        SINGLE,     // Único pago
        MONTHLY,    // Mensual
        QUARTERLY,  // Trimestral
        SEMI_ANNUAL, // Semestral
        ANNUAL      // Anual
    }

    public enum PlanStatus {
        ACTIVE,     // Plan activo
        COMPLETED,  // Plan completado
        CANCELLED,  // Plan cancelado
        OVERDUE,    // Plan con pagos atrasados
        SUSPENDED   // Plan suspendido
    }

    // Métodos para gestionar relaciones bidireccionales
    public void addPayment(Payment payment) {
        payments.add(payment);
        payment.setPaymentPlan(this);
    }

    public void removePayment(Payment payment) {
        payments.remove(payment);
        payment.setPaymentPlan(null);
    }
}
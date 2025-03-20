package com.insurtech.claim.model.entity;

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
@Table(name = "CLAIM_PAYMENTS")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ClaimPayment {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_CLAIM_PAYMENTS")
    @SequenceGenerator(name = "SEQ_CLAIM_PAYMENTS", sequenceName = "SEQ_CLAIM_PAYMENTS", allocationSize = 1)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "CLAIM_ID", nullable = false)
    @EqualsAndHashCode.Exclude
    private Claim claim;

    @Column(name = "PAYMENT_NUMBER", unique = true, nullable = false)
    private String paymentNumber;

    @Column(name = "AMOUNT", precision = 19, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "PAYMENT_METHOD", nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    @Column(name = "PAYMENT_STATUS", nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;

    @Column(name = "PAYMENT_DATE")
    private LocalDateTime paymentDate;

    @Column(name = "BENEFICIARY_NAME")
    private String beneficiaryName;

    @Column(name = "BENEFICIARY_BANK_ACCOUNT")
    private String beneficiaryBankAccount;

    @Column(name = "BENEFICIARY_BANK")
    private String beneficiaryBank;

    @Column(name = "TRANSACTION_ID")
    private String transactionId;

    @Column(name = "REFERENCE")
    private String reference;

    @Column(name = "NOTES", length = 1000)
    private String notes;

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

    public enum PaymentMethod {
        BANK_TRANSFER, CHECK, CASH, DEBIT_CARD, CREDIT_CARD, DIGITAL_WALLET, OTHER
    }

    public enum PaymentStatus {
        PENDING, PROCESSING, COMPLETED, FAILED, CANCELLED, RETURNED
    }
}
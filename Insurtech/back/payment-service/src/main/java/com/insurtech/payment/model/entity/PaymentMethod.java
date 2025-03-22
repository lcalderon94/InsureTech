package com.insurtech.payment.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.HashSet;
import java.util.Set;

/**
 * Entidad para gestionar los métodos de pago en el sistema
 */
@Entity
@Table(name = "PAYMENT_METHODS")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class PaymentMethod {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_PAYMENT_METHODS")
    @SequenceGenerator(name = "SEQ_PAYMENT_METHODS", sequenceName = "SEQ_PAYMENT_METHODS", allocationSize = 1)
    private Long id;

    @Column(name = "PAYMENT_METHOD_NUMBER", unique = true, nullable = false)
    private String paymentMethodNumber;

    @Column(name = "CUSTOMER_NUMBER", nullable = false)
    private String customerNumber;

    @Column(name = "METHOD_TYPE", nullable = false)
    @Enumerated(EnumType.STRING)
    private MethodType methodType;

    @Column(name = "METHOD_NAME", nullable = false)
    private String name;

    @Column(name = "IS_DEFAULT")
    private boolean isDefault;

    @Column(name = "IS_ACTIVE")
    private boolean isActive = true;

    @Column(name = "IS_VERIFIED")
    private boolean isVerified;

    // Campos específicos para tarjeta de crédito/débito
    @Column(name = "CARD_HOLDER_NAME")
    private String cardHolderName;

    @Column(name = "MASKED_CARD_NUMBER")
    private String maskedCardNumber;

    @Column(name = "CARD_TYPE")
    private String cardType;

    // Se mapean las columnas de mes y año de caducidad de la tarjeta
    @Column(name = "CARD_EXPIRY_MONTH")
    private Integer cardExpiryMonth;

    @Column(name = "CARD_EXPIRY_YEAR")
    private Integer cardExpiryYear;

    // Métodos transitorios para trabajar con YearMonth

    @Transient
    public YearMonth getCardExpiryDate() {
        if (cardExpiryMonth != null && cardExpiryYear != null) {
            return YearMonth.of(cardExpiryYear, cardExpiryMonth);
        }
        return null;
    }

    public void setCardExpiryDate(YearMonth expiry) {
        if (expiry != null) {
            this.cardExpiryMonth = expiry.getMonthValue();
            this.cardExpiryYear = expiry.getYear();
        } else {
            this.cardExpiryMonth = null;
            this.cardExpiryYear = null;
        }
    }

    // Campos para cuenta bancaria
    @Column(name = "BANK_NAME")
    private String bankName;

    @Column(name = "ACCOUNT_NUMBER")
    private String accountNumber;

    @Column(name = "ACCOUNT_HOLDER_NAME")
    private String accountHolderName;

    @Column(name = "ACCOUNT_TYPE")
    private String accountType;

    // Campos para monedero electrónico
    @Column(name = "WALLET_PROVIDER")
    private String walletProvider;

    @Column(name = "WALLET_ID")
    private String walletId;

    // Token para pagos seguros (encriptado)
    @Column(name = "PAYMENT_TOKEN")
    private String paymentToken;

    @Column(name = "TOKEN_EXPIRY_DATE")
    private LocalDateTime tokenExpiryDate;

    // Relaciones
    @OneToMany(mappedBy = "paymentMethod")
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
    public enum MethodType {
        CREDIT_CARD,      // Tarjeta de crédito
        DEBIT_CARD,       // Tarjeta de débito
        BANK_ACCOUNT,     // Cuenta bancaria
        ELECTRONIC_WALLET, // Monedero electrónico
        DIRECT_DEBIT,     // Débito directo
        PAYPAL,           // PayPal
        CASH              // Efectivo
    }
}

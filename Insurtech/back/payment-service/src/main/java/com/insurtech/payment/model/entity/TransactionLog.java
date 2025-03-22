package com.insurtech.payment.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "TRANSACTION_LOGS")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class TransactionLog {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_TRANSACTION_LOGS")
    @SequenceGenerator(name = "SEQ_TRANSACTION_LOGS", sequenceName = "SEQ_TRANSACTION_LOGS", allocationSize = 1)
    private Long id;

    @Column(name = "TRANSACTION_ID")
    private String transactionId;

    @Column(name = "PAYMENT_NUMBER")
    private String paymentNumber;

    @Column(name = "REFUND_NUMBER")
    private String refundNumber;

    @Column(name = "LOG_TYPE", nullable = false)
    @Enumerated(EnumType.STRING)
    private LogType logType;

    @Column(name = "ACTION", nullable = false)
    private String action;

    @Column(name = "STATUS_BEFORE")
    private String statusBefore;

    @Column(name = "STATUS_AFTER")
    private String statusAfter;

    @Column(name = "SOURCE_IP")
    private String sourceIp;

    @Column(name = "USER_AGENT")
    private String userAgent;

    // Se indica que este campo es un LOB para mapear el CLOB de la BD
    @Lob
    @Column(name = "DETAILS")
    private String details;

    @Column(name = "ERROR_MESSAGE")
    private String errorMessage;

    @Column(name = "CORRELATION_ID")
    private String correlationId;

    // Campos de auditoría
    @CreationTimestamp
    @Column(name = "CREATED_AT", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "CREATED_BY")
    private String createdBy;

    // Enumeraciones
    public enum LogType {
        PAYMENT_CREATION,     // Creación de pago
        PAYMENT_UPDATE,       // Actualización de pago
        PAYMENT_STATUS_CHANGE,// Cambio de estado de pago
        TRANSACTION_PROCESS,  // Procesamiento de transacción
        TRANSACTION_RESPONSE, // Respuesta de la pasarela de pago
        REFUND_REQUEST,       // Solicitud de reembolso
        REFUND_PROCESS,       // Procesamiento de reembolso
        ERROR,                // Error en el procesamiento
        SECURITY,             // Evento de seguridad
        SYSTEM                // Evento del sistema
    }
}

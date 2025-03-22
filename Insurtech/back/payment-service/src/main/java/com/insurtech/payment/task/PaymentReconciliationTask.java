package com.insurtech.payment.task;

import com.insurtech.payment.model.dto.PaymentDto;
import com.insurtech.payment.model.dto.TransactionDto;
import com.insurtech.payment.model.entity.Payment;
import com.insurtech.payment.model.entity.Transaction;
import com.insurtech.payment.repository.PaymentRepository;
import com.insurtech.payment.service.PaymentGatewayService;
import com.insurtech.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class PaymentReconciliationTask {

    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;
    private final PaymentGatewayService paymentGatewayService;

    private static final int BATCH_SIZE = 100;

    /**
     * Reconcilia pagos en estado PROCESSING comprobando su estado en la pasarela de pago
     * Se ejecuta cada 15 minutos
     */
    @Scheduled(fixedRateString = "${payment.reconciliation.interval:900000}")
    public void reconcileProcessingPayments() {
        log.info("Iniciando tarea de reconciliación de pagos en procesamiento");

        try {
            // Obtener pagos en estado PROCESSING
            Page<Payment> processingPayments = paymentRepository.findByStatus(
                    Payment.PaymentStatus.PROCESSING,
                    PageRequest.of(0, BATCH_SIZE)
            );

            if (processingPayments.isEmpty()) {
                log.info("No hay pagos en estado PROCESSING para reconciliar");
                return;
            }

            log.info("Reconciliando {} pagos en estado PROCESSING", processingPayments.getTotalElements());

            // Procesar cada pago
            for (Payment payment : processingPayments) {
                try {
                    reconcilePayment(payment);
                } catch (Exception e) {
                    log.error("Error al reconciliar pago ID {}: {}", payment.getId(), e.getMessage());
                }
            }

            log.info("Tarea de reconciliación completada");
        } catch (Exception e) {
            log.error("Error en tarea de reconciliación: {}", e.getMessage());
        }
    }

    /**
     * Reconcilia un pago específico con la pasarela de pago
     */
    private void reconcilePayment(Payment payment) {
        log.debug("Reconciliando pago ID: {}", payment.getId());

        // Obtener la última transacción
        Transaction lastTransaction = payment.getTransactions().stream()
                .filter(t -> t.getTransactionType() == Transaction.TransactionType.PAYMENT)
                .max(java.util.Comparator.comparing(Transaction::getTransactionDate))
                .orElse(null);

        if (lastTransaction == null) {
            log.warn("No se encontró transacción para el pago ID: {}", payment.getId());
            return;
        }

        // Verificar el estado en la pasarela de pago
        Transaction.TransactionStatus gatewayStatus =
                paymentGatewayService.checkTransactionStatus(lastTransaction.getTransactionId());

        log.debug("Estado de transacción {} en pasarela: {}", lastTransaction.getTransactionId(), gatewayStatus);

        // Actualizar el estado del pago según corresponda
        if (gatewayStatus == Transaction.TransactionStatus.SUCCESSFUL) {
            paymentService.updatePaymentStatus(
                    payment.getId(),
                    Payment.PaymentStatus.COMPLETED,
                    "Pago completado según reconciliación con pasarela"
            );
            log.info("Pago ID {} actualizado a COMPLETED por reconciliación", payment.getId());
        } else if (gatewayStatus == Transaction.TransactionStatus.FAILED) {
            paymentService.updatePaymentStatus(
                    payment.getId(),
                    Payment.PaymentStatus.FAILED,
                    "Pago fallido según reconciliación con pasarela"
            );
            log.info("Pago ID {} actualizado a FAILED por reconciliación", payment.getId());
        } else if (payment.getCreationDate().plusHours(24).isBefore(LocalDateTime.now())) {
            // Si ha pasado más de 24 horas y sigue en PENDING o PROCESSING, marcar como expirado
            paymentService.updatePaymentStatus(
                    payment.getId(),
                    Payment.PaymentStatus.EXPIRED,
                    "Pago expirado tras 24 horas sin confirmación"
            );
            log.info("Pago ID {} marcado como EXPIRED por tiempo excedido", payment.getId());
        }
    }
}
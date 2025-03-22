package com.insurtech.payment.task;

import com.insurtech.payment.model.dto.PaymentDto;
import com.insurtech.payment.model.dto.PaymentMethodDto;
import com.insurtech.payment.model.entity.Payment;
import com.insurtech.payment.model.entity.PaymentMethod;
import com.insurtech.payment.repository.PaymentMethodRepository;
import com.insurtech.payment.repository.PaymentRepository;
import com.insurtech.payment.service.PaymentBatchService;
import com.insurtech.payment.service.PaymentMethodService;
import com.insurtech.payment.service.PaymentService;
import com.insurtech.payment.util.EntityDtoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor
public class FailedPaymentRetryTask {

    private final PaymentRepository paymentRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final PaymentBatchService paymentBatchService;
    private final PaymentMethodService paymentMethodService;
    private final EntityDtoMapper mapper;

    @Value("${payment.retry.max-attempts:3}")
    private int maxRetryAttempts;

    @Value("${payment.retry.interval-hours:24}")
    private int retryIntervalHours;

    @Value("${payment.retry.batch-size:50}")
    private int batchSize;

    /**
     * Reintenta pagos fallidos después de un intervalo de tiempo
     * Se ejecuta cada 4 horas
     */
    @Scheduled(fixedRateString = "${payment.retry.schedule-interval:14400000}")
    public void retryFailedPayments() {
        log.info("Iniciando tarea de reintento de pagos fallidos");

        try {
            // Obtener pagos fallidos que son elegibles para reintento
            LocalDateTime cutoffTime = LocalDateTime.now().minusHours(retryIntervalHours);

            // Pagos que han fallado, no han superado el máximo de reintentos y su último intento fue hace más del intervalo
            Page<Payment> eligiblePayments = paymentRepository.findFailedPaymentsEligibleForRetry(
                    Payment.PaymentStatus.FAILED,
                    maxRetryAttempts,
                    cutoffTime,
                    PageRequest.of(0, batchSize)
            );

            if (eligiblePayments.isEmpty()) {
                log.info("No hay pagos fallidos elegibles para reintento");
                return;
            }

            log.info("Encontrados {} pagos fallidos elegibles para reintento", eligiblePayments.getTotalElements());

            // Procesar cada pago
            for (Payment payment : eligiblePayments) {
                try {
                    retryPayment(payment);
                } catch (Exception e) {
                    log.error("Error al reintentar pago ID {}: {}", payment.getId(), e.getMessage());
                }
            }

            log.info("Tarea de reintento de pagos fallidos completada");
        } catch (Exception e) {
            log.error("Error en tarea de reintento de pagos fallidos: {}", e.getMessage());
        }
    }

    /**
     * Reintenta un pago específico
     */
    private void retryPayment(Payment payment) {
        log.debug("Reintentando pago ID: {}, intento {}/{}", payment.getId(), payment.getRetryCount() + 1, maxRetryAttempts);

        // Incrementar contador de reintentos
        payment.setRetryCount(payment.getRetryCount() + 1);
        payment.setLastRetryDate(LocalDateTime.now());
        paymentRepository.save(payment);

        // Buscar un método de pago por defecto para el cliente
        Optional<PaymentMethod> defaultPaymentMethod =
                paymentMethodRepository.findDefaultByCustomerNumber(payment.getCustomerNumber());

        if (defaultPaymentMethod.isEmpty()) {
            log.warn("No se encontró método de pago por defecto para el cliente: {}", payment.getCustomerNumber());
            return;
        }

        // Convertir a DTO y reintentar el pago
        PaymentMethodDto paymentMethodDto = mapper.mapToDto(defaultPaymentMethod.get(), PaymentMethodDto.class);

        try {
            // Cambiar el estado a PENDING antes de reintentar
            payment.setStatus(Payment.PaymentStatus.PENDING);
            payment = paymentRepository.save(payment);

            // Reintentar el pago
            PaymentDto paymentDto = mapper.mapToDto(payment, PaymentDto.class);
            paymentBatchService.processPendingPaymentsBatch(paymentMethodDto);

            log.info("Reintento de pago ID {} iniciado correctamente", payment.getId());
        } catch (Exception e) {
            log.error("Error al reintentar el pago ID {}: {}", payment.getId(), e.getMessage());

            // Volver a marcar como fallido
            payment.setStatus(Payment.PaymentStatus.FAILED);
            payment.setFailureReason("Error en reintento automático: " + e.getMessage());
            paymentRepository.save(payment);
        }
    }
}
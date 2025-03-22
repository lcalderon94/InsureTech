package com.insurtech.payment.service.async;

import com.insurtech.payment.event.producer.PaymentEventProducer;
import com.insurtech.payment.model.dto.PaymentDto;
import com.insurtech.payment.model.entity.Payment;
import com.insurtech.payment.service.DistributedLockService;
import com.insurtech.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Component
@Slf4j
@RequiredArgsConstructor
public class DistributedTransactionHandler {

    private final PaymentService paymentService;
    private final PaymentEventProducer paymentEventProducer;
    private final DistributedLockService lockService;

    // Almacén en memoria para transacciones en progreso
    private final Map<String, TransactionStatus> transactionStatusMap = new ConcurrentHashMap<>();

    private static final int DEFAULT_TIMEOUT_SECONDS = 60;

    /**
     * Ejecuta una operación transaccional distribuida de forma segura
     */
    public <T> T executeDistributedTransaction(
            String transactionId,
            PaymentDto paymentDto,
            Function<PaymentDto, T> operation,
            Consumer<Throwable> compensationAction) {

        String lockKey = "transaction:" + transactionId;

        try {
            // Adquirir bloqueo distribuido
            return lockService.executeWithLock(lockKey, () -> {
                // Registrar transacción como iniciada
                transactionStatusMap.put(transactionId, TransactionStatus.STARTED);

                // Publicar evento de inicio de transacción
                paymentEventProducer.publishTransactionStartedEvent(transactionId, paymentDto);

                try {
                    // Ejecutar la operación principal
                    T result = operation.apply(paymentDto);

                    // Marcar como completado
                    transactionStatusMap.put(transactionId, TransactionStatus.COMPLETED);

                    // Publicar evento de finalización exitosa
                    paymentEventProducer.publishTransactionCompletedEvent(transactionId, paymentDto);

                    return result;
                } catch (Exception e) {
                    log.error("Error en transacción distribuida {}: {}", transactionId, e.getMessage());

                    // Marcar como fallido
                    transactionStatusMap.put(transactionId, TransactionStatus.FAILED);

                    // Ejecutar compensación
                    try {
                        compensationAction.accept(e);
                    } catch (Exception compensationError) {
                        log.error("Error en acción de compensación para transacción {}: {}",
                                transactionId, compensationError.getMessage());
                    }

                    // Publicar evento de fallo
                    paymentEventProducer.publishTransactionFailedEvent(transactionId, paymentDto, e.getMessage());

                    throw e;
                } finally {
                    // Programar limpieza del estado después de un tiempo
                    scheduleStatusCleanup(transactionId);
                }
            });
        } catch (Exception e) {
            log.error("Error al ejecutar transacción distribuida {}: {}", transactionId, e.getMessage());
            throw e;
        }
    }

    /**
     * Verifica el estado de una transacción distribuida
     */
    public TransactionStatus checkTransactionStatus(String transactionId) {
        return transactionStatusMap.getOrDefault(transactionId, TransactionStatus.UNKNOWN);
    }

    /**
     * Programa la limpieza del estado de la transacción después de un tiempo
     */
    private void scheduleStatusCleanup(String transactionId) {
        // En un entorno real, se utilizaría un Scheduler o un job programado
        // Para simplificar, usamos un Thread que limpiará después de un tiempo
        new Thread(() -> {
            try {
                TimeUnit.SECONDS.sleep(DEFAULT_TIMEOUT_SECONDS);
                transactionStatusMap.remove(transactionId);
                log.debug("Estado de transacción {} limpiado del mapa", transactionId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    /**
     * Estados posibles de una transacción distribuida
     */
    public enum TransactionStatus {
        UNKNOWN,
        STARTED,
        COMPLETED,
        FAILED
    }
}
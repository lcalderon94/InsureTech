package com.insurtech.payment.service.async;

import com.insurtech.payment.event.producer.PaymentEventProducer;
import com.insurtech.payment.exception.PaymentProcessingException;
import com.insurtech.payment.model.dto.PaymentDto;
import com.insurtech.payment.model.dto.PaymentMethodDto;
import com.insurtech.payment.model.entity.Payment;
import com.insurtech.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@Slf4j
@RequiredArgsConstructor
public class AsyncPaymentProcessor {

    private final PaymentService paymentService;
    private final PaymentEventProducer paymentEventProducer;

    @Value("${payment.batch.concurrency.threshold:10}")
    private int concurrencyThreshold;

    @Value("${payment.batch.concurrency.max-threads:5}")
    private int maxThreads;

    /**
     * Procesa un pago de forma asíncrona
     */
    @Async
    public CompletableFuture<PaymentDto> processPaymentAsync(Long paymentId, PaymentMethodDto paymentMethodDto) {
        log.info("Iniciando procesamiento asíncrono de pago ID: {}", paymentId);

        try {
            // Procesar el pago
            PaymentDto processedPayment = paymentService.processPayment(paymentId, paymentMethodDto);

            // Publicar evento según el resultado
            if (processedPayment.getStatus() == Payment.PaymentStatus.COMPLETED) {
                paymentEventProducer.publishPaymentProcessedEvent(processedPayment);
            } else if (processedPayment.getStatus() == Payment.PaymentStatus.FAILED) {
                paymentEventProducer.publishPaymentFailedEvent(processedPayment);
            }

            log.info("Procesamiento asíncrono de pago ID: {} completado con estado: {}",
                    paymentId, processedPayment.getStatus());

            return CompletableFuture.completedFuture(processedPayment);
        } catch (Exception e) {
            log.error("Error en procesamiento asíncrono de pago ID: {}: {}", paymentId, e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Procesa un lote de pagos de forma asíncrona y paralelizada
     */
    public CompletableFuture<List<PaymentDto>> processBatchPaymentsAsync(List<Long> paymentIds,
                                                                         PaymentMethodDto paymentMethodDto) {
        log.info("Iniciando procesamiento por lotes de {} pagos", paymentIds.size());

        try {
            if (paymentIds.size() > concurrencyThreshold) {
                log.info("Utilizando procesamiento paralelo para lote de pagos (tamaño > {})", concurrencyThreshold);

                // Crear un pool de hilos para procesamiento paralelo
                ExecutorService executorService = Executors.newFixedThreadPool(
                        Math.min(maxThreads, paymentIds.size() / 2 + 1)
                );

                try {
                    // Convertir cada pago en una tarea CompletableFuture
                    List<CompletableFuture<PaymentDto>> futures = paymentIds.stream()
                            .map(paymentId -> CompletableFuture.supplyAsync(() -> {
                                try {
                                    return paymentService.processPayment(paymentId, paymentMethodDto);
                                } catch (Exception e) {
                                    log.error("Error al procesar pago ID: {} en lote: {}", paymentId, e.getMessage());
                                    throw new PaymentProcessingException("Error en procesamiento por lotes para pago ID: " +
                                            paymentId + ": " + e.getMessage());
                                }
                            }, executorService))
                            .toList();

                    // Esperar a que todos los pagos sean procesados
                    CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                            futures.toArray(new CompletableFuture[0])
                    );

                    // Cuando todos los futures se completen, obtener los resultados
                    return allFutures.thenApply(v ->
                            futures.stream()
                                    .map(CompletableFuture::join)
                                    .toList()
                    );
                } finally {
                    executorService.shutdown();
                }
            } else {
                log.info("Utilizando procesamiento secuencial para lote de pagos (tamaño <= {})", concurrencyThreshold);

                // Procesar los pagos secuencialmente
                List<PaymentDto> results = paymentIds.stream()
                        .map(paymentId -> {
                            try {
                                return paymentService.processPayment(paymentId, paymentMethodDto);
                            } catch (Exception e) {
                                log.error("Error al procesar pago ID: {} en lote: {}", paymentId, e.getMessage());
                                throw new PaymentProcessingException("Error en procesamiento por lotes para pago ID: " +
                                        paymentId + ": " + e.getMessage());
                            }
                        })
                        .toList();

                return CompletableFuture.completedFuture(results);
            }
        } catch (Exception e) {
            log.error("Error general en procesamiento por lotes: {}", e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }
}
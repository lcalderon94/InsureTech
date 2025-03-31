package com.insurtech.notification.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Calculador de tiempos de espera para reintentos con backoff exponencial
 */
@Component
public class RetryBackoffCalculator {

    @Value("${notification.retry.initial-interval:1000}")
    private long initialIntervalMs;

    @Value("${notification.retry.multiplier:2.0}")
    private double multiplier;

    @Value("${notification.retry.max-interval:60000}")
    private long maxIntervalMs;

    /**
     * Calcula el tiempo de espera para un reintento basado en backoff exponencial
     *
     * @param attemptNumber número de intento (empezando en 1)
     * @return tiempo de espera en milisegundos
     */
    public long calculateBackoffMillis(int attemptNumber) {
        if (attemptNumber < 1) {
            return 0;
        }

        // Fórmula de backoff exponencial: initialInterval * (multiplier ^ (attemptNumber - 1))
        double delay = initialIntervalMs * Math.pow(multiplier, attemptNumber - 1);

        // Aplicar jitter para evitar sincronización de reintentos
        double jitter = 0.1; // 10% de variación aleatoria
        delay = delay * (1.0 + jitter * (Math.random() - 0.5));

        // Limitar al máximo
        return Math.min(Math.round(delay), maxIntervalMs);
    }

    /**
     * Calcula el número máximo de reintentos para un período de tiempo dado
     *
     * @param maxTotalTimeMs tiempo total máximo en milisegundos
     * @return número máximo de reintentos
     */
    public int calculateMaxRetries(long maxTotalTimeMs) {
        int attempts = 0;
        long cumulativeTime = 0;

        while (cumulativeTime < maxTotalTimeMs) {
            attempts++;
            long nextBackoff = calculateBackoffMillis(attempts);
            cumulativeTime += nextBackoff;

            if (nextBackoff >= maxIntervalMs) {
                // Si alcanzamos el intervalo máximo, calcular cuántos intentos más podemos hacer
                long remainingTime = maxTotalTimeMs - cumulativeTime;
                attempts += remainingTime / maxIntervalMs;
                break;
            }
        }

        return attempts;
    }
}
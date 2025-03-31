package com.insurtech.notification.util;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Generador de números secuenciales para notificaciones y lotes
 */
@Component
public class NotificationNumberGenerator {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String NOTIFICATION_PREFIX = "NOT";
    private static final String BATCH_PREFIX = "BAT";

    private final AtomicInteger notificationCounter = new AtomicInteger(1);
    private final AtomicInteger batchCounter = new AtomicInteger(1);
    private final Random random = new Random();

    /**
     * Genera un número único para una notificación
     * Formato: NOT-YYYYMMDD-SECUENCIAL-RANDOM
     */
    public String generateNotificationNumber() {
        String datePart = LocalDateTime.now().format(DATE_FORMAT);
        String sequentialPart = String.format("%06d", notificationCounter.getAndIncrement());
        String randomPart = String.format("%04d", random.nextInt(10000));

        return NOTIFICATION_PREFIX + "-" + datePart + "-" + sequentialPart + "-" + randomPart;
    }

    /**
     * Genera un número único para un lote de notificaciones
     * Formato: BAT-YYYYMMDD-SECUENCIAL-RANDOM
     */
    public String generateBatchNumber() {
        String datePart = LocalDateTime.now().format(DATE_FORMAT);
        String sequentialPart = String.format("%04d", batchCounter.getAndIncrement());
        String randomPart = String.format("%04d", random.nextInt(10000));

        return BATCH_PREFIX + "-" + datePart + "-" + sequentialPart + "-" + randomPart;
    }
}
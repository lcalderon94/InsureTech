package com.insurtech.notification.scheduler;

import com.insurtech.notification.service.interfaces.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Programador de tareas para reintentar notificaciones fallidas
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RetryScheduler {

    private final NotificationService notificationService;

    /**
     * Procesa notificaciones pendientes de reintento cada minuto
     */
    @Scheduled(cron = "0 * * * * *") // Cada minuto
    public void processRetryNotifications() {
        log.debug("Iniciando procesamiento de reintentos programados");

        try {
            int processedCount = notificationService.processRetryNotifications();

            if (processedCount > 0) {
                log.info("Procesadas {} notificaciones para reintento", processedCount);
            }
        } catch (Exception e) {
            log.error("Error procesando reintentos: {}", e.getMessage(), e);
        }
    }

    /**
     * Procesa notificaciones programadas cada minuto
     */
    @Scheduled(cron = "30 * * * * *") // Cada minuto, con 30 segundos de desplazamiento
    public void processScheduledNotifications() {
        log.debug("Iniciando procesamiento de notificaciones programadas");

        try {
            int processedCount = notificationService.processScheduledNotifications();

            if (processedCount > 0) {
                log.info("Procesadas {} notificaciones programadas", processedCount);
            }
        } catch (Exception e) {
            log.error("Error procesando notificaciones programadas: {}", e.getMessage(), e);
        }
    }
}
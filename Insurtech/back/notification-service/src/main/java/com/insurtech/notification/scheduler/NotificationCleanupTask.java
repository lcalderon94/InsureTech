package com.insurtech.notification.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Tarea programada para limpiar notificaciones antiguas
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationCleanupTask {

    private final JdbcTemplate jdbcTemplate;

    private static final int RETENTION_DAYS = 90; // Retención de datos (3 meses)

    /**
     * Limpia notificaciones antiguas cada día a las 2:30 AM
     */
    @Scheduled(cron = "0 30 2 * * *") // 2:30 AM todos los días
    @Transactional
    public void cleanupOldNotifications() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(RETENTION_DAYS);
        String formattedDate = cutoffDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        log.info("Iniciando limpieza de notificaciones anteriores a {}", formattedDate);

        try {
            // Obtener IDs de notificaciones a eliminar
            int countToDelete = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM NOTIFICATIONS WHERE CREATED_AT < ?",
                    Integer.class,
                    cutoffDate);

            log.info("Se eliminarán {} notificaciones antiguas", countToDelete);

            if (countToDelete > 0) {
                // Primero eliminamos los intentos de entrega asociados
                int deliveryAttemptsDeleted = jdbcTemplate.update(
                        "DELETE FROM DELIVERY_ATTEMPTS WHERE NOTIFICATION_ID IN " +
                                "(SELECT ID FROM NOTIFICATIONS WHERE CREATED_AT < ?)",
                        cutoffDate);

                // Después eliminamos las notificaciones
                int notificationsDeleted = jdbcTemplate.update(
                        "DELETE FROM NOTIFICATIONS WHERE CREATED_AT < ?",
                        cutoffDate);

                log.info("Limpieza completada: {} intentos de entrega y {} notificaciones eliminadas",
                        deliveryAttemptsDeleted, notificationsDeleted);
            }
        } catch (Exception e) {
            log.error("Error durante la limpieza de notificaciones: {}", e.getMessage(), e);
        }
    }

    /**
     * Limpia lotes procesados antiguos cada semana a las 3:30 AM del domingo
     */
    @Scheduled(cron = "0 30 3 * * 0") // 3:30 AM cada domingo
    @Transactional
    public void cleanupOldBatches() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(RETENTION_DAYS);
        String formattedDate = cutoffDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        log.info("Iniciando limpieza de lotes anteriores a {}", formattedDate);

        try {
            // Obtener IDs de lotes completados a eliminar
            int countToDelete = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM NOTIFICATION_BATCHES WHERE COMPLETED_AT < ? " +
                            "AND STATUS IN ('SENT', 'FAILED', 'PARTIALLY_SENT', 'CANCELLED')",
                    Integer.class,
                    cutoffDate);

            log.info("Se eliminarán {} lotes antiguos", countToDelete);

            if (countToDelete > 0) {
                // Eliminamos los lotes
                int batchesDeleted = jdbcTemplate.update(
                        "DELETE FROM NOTIFICATION_BATCHES WHERE COMPLETED_AT < ? " +
                                "AND STATUS IN ('SENT', 'FAILED', 'PARTIALLY_SENT', 'CANCELLED')",
                        cutoffDate);

                log.info("Limpieza completada: {} lotes eliminados", batchesDeleted);
            }
        } catch (Exception e) {
            log.error("Error durante la limpieza de lotes: {}", e.getMessage(), e);
        }
    }
}
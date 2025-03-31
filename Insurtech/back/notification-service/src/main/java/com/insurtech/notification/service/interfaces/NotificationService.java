package com.insurtech.notification.service.interfaces;

import com.insurtech.notification.model.dto.BatchRequestDto;
import com.insurtech.notification.model.dto.NotificationRequestDto;
import com.insurtech.notification.model.dto.NotificationResponseDto;
import com.insurtech.notification.model.entity.Notification;
import com.insurtech.notification.model.entity.NotificationBatch;
import com.insurtech.notification.model.enums.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationService {

    /**
     * Crea una nueva notificación
     *
     * @param requestDto datos de la notificación
     * @return respuesta con los datos de la notificación creada
     */
    NotificationResponseDto createNotification(NotificationRequestDto requestDto);

    /**
     * Crea un lote de notificaciones
     *
     * @param batchRequestDto datos del lote de notificaciones
     * @return identificador del lote creado
     */
    UUID createBatch(BatchRequestDto batchRequestDto);

    /**
     * Busca una notificación por su ID
     *
     * @param id identificador de la notificación
     * @return notificación si existe
     */
    Optional<NotificationResponseDto> findNotificationById(UUID id);

    /**
     * Busca una notificación por su número
     *
     * @param notificationNumber número de notificación
     * @return notificación si existe
     */
    Optional<NotificationResponseDto> findNotificationByNumber(String notificationNumber);

    /**
     * Obtiene todas las notificaciones con paginación
     *
     * @param pageable información de paginación
     * @return página de notificaciones
     */
    Page<NotificationResponseDto> findAllNotifications(Pageable pageable);

    /**
     * Busca notificaciones con filtros
     *
     * @param recipient destinatario (parcial)
     * @param subject asunto (parcial)
     * @param status estado de la notificación
     * @param type tipo de notificación
     * @param fromDate fecha desde
     * @param toDate fecha hasta
     * @param pageable información de paginación
     * @return página de notificaciones filtradas
     */
    Page<NotificationResponseDto> searchNotifications(
            String recipient,
            String subject,
            NotificationStatus status,
            String type,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            Pageable pageable);

    /**
     * Cancela una notificación en estado pendiente
     *
     * @param id identificador de la notificación
     * @return true si se canceló correctamente
     */
    boolean cancelNotification(UUID id);

    /**
     * Reintenta el envío de una notificación fallida
     *
     * @param id identificador de la notificación
     * @return notificación actualizada
     */
    NotificationResponseDto retryNotification(UUID id);

    /**
     * Procesa el envío de una notificación
     *
     * @param notification notificación a procesar
     * @return true si se procesó correctamente
     */
    boolean processNotification(Notification notification);

    /**
     * Procesa notificaciones programadas que están pendientes
     *
     * @return número de notificaciones procesadas
     */
    int processScheduledNotifications();

    /**
     * Procesa notificaciones pendientes de reintento
     *
     * @return número de notificaciones procesadas
     */
    int processRetryNotifications();

    /**
     * Obtiene estadísticas de notificaciones
     *
     * @return mapa con estadísticas
     */
    List<NotificationResponseDto> findBySourceEventId(String sourceEventId);

    /**
     * Obtiene información de un lote de notificaciones
     *
     * @param batchId identificador del lote
     * @return lote si existe
     */
    Optional<NotificationBatch> findBatchById(UUID batchId);

    /**
     * Actualiza el estado de un lote de notificaciones
     *
     * @param batchId identificador del lote
     * @return lote actualizado
     */
    NotificationBatch updateBatchStatus(UUID batchId);
}
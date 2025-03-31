package com.insurtech.notification.controller;

import com.insurtech.notification.model.dto.NotificationRequestDto;
import com.insurtech.notification.model.dto.NotificationResponseDto;
import com.insurtech.notification.model.enums.NotificationStatus;
import com.insurtech.notification.service.interfaces.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Notificaciones", description = "API para gestionar notificaciones")
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping
    @Operation(summary = "Crear notificación", description = "Crea una nueva notificación para su envío")
    @PreAuthorize("hasAnyRole('ADMIN', 'NOTIFICATION_ADMIN', 'NOTIFICATION_SENDER')")
    public ResponseEntity<NotificationResponseDto> createNotification(
            @Valid @RequestBody NotificationRequestDto requestDto) {
        log.info("Solicitud para crear notificación recibida");
        NotificationResponseDto response = notificationService.createNotification(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener notificación por ID", description = "Obtiene el detalle de una notificación por su ID")
    @PreAuthorize("hasAnyRole('ADMIN', 'NOTIFICATION_ADMIN', 'NOTIFICATION_VIEWER')")
    public ResponseEntity<NotificationResponseDto> getNotificationById(
            @PathVariable("id") UUID id) {
        log.info("Solicitud para obtener notificación con ID: {}", id);
        return notificationService.findNotificationById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/number/{notificationNumber}")
    @Operation(summary = "Obtener notificación por número", description = "Obtiene el detalle de una notificación por su número")
    @PreAuthorize("hasAnyRole('ADMIN', 'NOTIFICATION_ADMIN', 'NOTIFICATION_VIEWER')")
    public ResponseEntity<NotificationResponseDto> getNotificationByNumber(
            @PathVariable("notificationNumber") String notificationNumber) {
        log.info("Solicitud para obtener notificación con número: {}", notificationNumber);
        return notificationService.findNotificationByNumber(notificationNumber)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @Operation(summary = "Listar notificaciones", description = "Obtiene un listado paginado de notificaciones")
    @PreAuthorize("hasAnyRole('ADMIN', 'NOTIFICATION_ADMIN', 'NOTIFICATION_VIEWER')")
    public ResponseEntity<Page<NotificationResponseDto>> getAllNotifications(Pageable pageable) {
        log.info("Solicitud para listar notificaciones: página {}, tamaño {}",
                pageable.getPageNumber(), pageable.getPageSize());
        Page<NotificationResponseDto> notifications = notificationService.findAllNotifications(pageable);
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/search")
    @Operation(summary = "Buscar notificaciones", description = "Busca notificaciones con filtros")
    @PreAuthorize("hasAnyRole('ADMIN', 'NOTIFICATION_ADMIN', 'NOTIFICATION_VIEWER')")
    public ResponseEntity<Page<NotificationResponseDto>> searchNotifications(
            @RequestParam(required = false) String recipient,
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) NotificationStatus status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            Pageable pageable) {
        log.info("Solicitud para buscar notificaciones con filtros");
        Page<NotificationResponseDto> notifications = notificationService.searchNotifications(
                recipient, subject, status, type, fromDate, toDate, pageable);
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/event/{sourceEventId}")
    @Operation(summary = "Obtener notificaciones por evento", description = "Obtiene notificaciones asociadas a un ID de evento")
    @PreAuthorize("hasAnyRole('ADMIN', 'NOTIFICATION_ADMIN', 'NOTIFICATION_VIEWER')")
    public ResponseEntity<List<NotificationResponseDto>> getNotificationsBySourceEventId(
            @PathVariable("sourceEventId") String sourceEventId) {
        log.info("Solicitud para obtener notificaciones para el evento: {}", sourceEventId);
        List<NotificationResponseDto> notifications = notificationService.findBySourceEventId(sourceEventId);
        return ResponseEntity.ok(notifications);
    }

    @PostMapping("/{id}/retry")
    @Operation(summary = "Reintentar notificación", description = "Reintenta enviar una notificación fallida")
    @PreAuthorize("hasAnyRole('ADMIN', 'NOTIFICATION_ADMIN')")
    public ResponseEntity<NotificationResponseDto> retryNotification(
            @PathVariable("id") UUID id) {
        log.info("Solicitud para reintentar notificación con ID: {}", id);
        NotificationResponseDto response = notificationService.retryNotification(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancelar notificación", description = "Cancela una notificación pendiente")
    @PreAuthorize("hasAnyRole('ADMIN', 'NOTIFICATION_ADMIN')")
    public ResponseEntity<Void> cancelNotification(
            @PathVariable("id") UUID id) {
        log.info("Solicitud para cancelar notificación con ID: {}", id);
        boolean cancelled = notificationService.cancelNotification(id);

        if (cancelled) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/webhook/delivery-status")
    @Operation(summary = "Webhook para estado de entrega", description = "Endpoint para recibir actualizaciones de estado de entrega desde proveedores externos")
    public ResponseEntity<Void> handleDeliveryStatusWebhook(
            @RequestParam String provider,
            @RequestParam String messageId,
            @RequestParam String status) {
        log.info("Webhook de estado de entrega recibido: proveedor={}, messageId={}, status={}",
                provider, messageId, status);
        // Aquí se implementaría la lógica para actualizar el estado de la notificación
        // basado en la retroalimentación del proveedor
        return ResponseEntity.ok().build();
    }
}
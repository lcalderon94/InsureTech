package com.insurtech.notification.controller;

import com.insurtech.notification.model.dto.BatchRequestDto;
import com.insurtech.notification.model.entity.NotificationBatch;
import com.insurtech.notification.model.enums.NotificationStatus;
import com.insurtech.notification.service.interfaces.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
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
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications/batches")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Lotes de Notificaciones", description = "API para gestionar lotes de notificaciones")
public class NotificationBatchController {

    private final NotificationService notificationService;

    @PostMapping
    @Operation(summary = "Crear lote de notificaciones", description = "Crea un nuevo lote para envío masivo de notificaciones")
    @PreAuthorize("hasAnyRole('ADMIN', 'NOTIFICATION_ADMIN')")
    public ResponseEntity<UUID> createBatch(
            @Valid @RequestBody BatchRequestDto batchRequestDto) {
        log.info("Solicitud para crear lote con {} notificaciones",
                batchRequestDto.getNotifications().size());
        UUID batchId = notificationService.createBatch(batchRequestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(batchId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener lote por ID", description = "Obtiene el detalle de un lote por su ID")
    @PreAuthorize("hasAnyRole('ADMIN', 'NOTIFICATION_ADMIN', 'NOTIFICATION_VIEWER')")
    public ResponseEntity<NotificationBatch> getBatchById(
            @PathVariable("id") UUID id) {
        log.info("Solicitud para obtener lote con ID: {}", id);
        return notificationService.findBatchById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    @Operation(summary = "Buscar lotes", description = "Busca lotes de notificaciones con filtros")
    @PreAuthorize("hasAnyRole('ADMIN', 'NOTIFICATION_ADMIN', 'NOTIFICATION_VIEWER')")
    public ResponseEntity<Page<NotificationBatch>> searchBatches(
            @RequestParam(required = false) String batchNumber,
            @RequestParam(required = false) NotificationStatus status,
            @RequestParam(required = false) String sourceReference,
            @RequestParam(required = false) String sourceType,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            Pageable pageable) {
        // Aquí se implementaría la lógica para buscar lotes con filtros
        // Similar al método searchNotifications del NotificationService
        log.info("Solicitud para buscar lotes con filtros");
        return ResponseEntity.ok(Page.empty(pageable));
    }

    @PostMapping("/{id}/update-status")
    @Operation(summary = "Actualizar estado del lote", description = "Fuerza la actualización del estado de un lote")
    @PreAuthorize("hasAnyRole('ADMIN', 'NOTIFICATION_ADMIN')")
    public ResponseEntity<NotificationBatch> updateBatchStatus(
            @PathVariable("id") UUID id) {
        log.info("Solicitud para actualizar estado del lote con ID: {}", id);
        NotificationBatch updatedBatch = notificationService.updateBatchStatus(id);
        return ResponseEntity.ok(updatedBatch);
    }
}
package com.insurtech.notification.controller;

import com.insurtech.notification.model.dto.TemplateDto;
import com.insurtech.notification.model.enums.NotificationType;
import com.insurtech.notification.service.interfaces.TemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications/templates")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Plantillas de Notificaciones", description = "API para gestionar plantillas de notificaciones")
public class NotificationTemplateController {

    private final TemplateService templateService;

    @PostMapping
    @Operation(summary = "Crear plantilla", description = "Crea una nueva plantilla de notificación")
    @PreAuthorize("hasAnyRole('ADMIN', 'NOTIFICATION_ADMIN')")
    public ResponseEntity<TemplateDto> createTemplate(
            @Valid @RequestBody TemplateDto templateDto) {
        log.info("Solicitud para crear plantilla: {}", templateDto.getCode());
        TemplateDto createdTemplate = templateService.createTemplate(templateDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdTemplate);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar plantilla", description = "Actualiza una plantilla existente")
    @PreAuthorize("hasAnyRole('ADMIN', 'NOTIFICATION_ADMIN')")
    public ResponseEntity<TemplateDto> updateTemplate(
            @PathVariable("id") UUID id,
            @Valid @RequestBody TemplateDto templateDto) {
        log.info("Solicitud para actualizar plantilla con ID: {}", id);
        TemplateDto updatedTemplate = templateService.updateTemplate(id, templateDto);
        return ResponseEntity.ok(updatedTemplate);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener plantilla por ID", description = "Obtiene el detalle de una plantilla por su ID")
    @PreAuthorize("hasAnyRole('ADMIN', 'NOTIFICATION_ADMIN', 'NOTIFICATION_VIEWER')")
    public ResponseEntity<TemplateDto> getTemplateById(
            @PathVariable("id") UUID id) {
        log.info("Solicitud para obtener plantilla con ID: {}", id);
        return templateService.findTemplateById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @Operation(summary = "Listar plantillas", description = "Obtiene un listado paginado de plantillas")
    @PreAuthorize("hasAnyRole('ADMIN', 'NOTIFICATION_ADMIN', 'NOTIFICATION_VIEWER')")
    public ResponseEntity<Page<TemplateDto>> getAllTemplates(Pageable pageable) {
        log.info("Solicitud para listar plantillas: página {}, tamaño {}",
                pageable.getPageNumber(), pageable.getPageSize());
        Page<TemplateDto> templates = templateService.findAllTemplates(pageable);
        return ResponseEntity.ok(templates);
    }

    @GetMapping("/search")
    @Operation(summary = "Buscar plantillas", description = "Busca plantillas con filtros")
    @PreAuthorize("hasAnyRole('ADMIN', 'NOTIFICATION_ADMIN', 'NOTIFICATION_VIEWER')")
    public ResponseEntity<Page<TemplateDto>> searchTemplates(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) NotificationType type,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String eventType,
            Pageable pageable) {
        log.info("Solicitud para buscar plantillas con filtros");
        Page<TemplateDto> templates = templateService.searchTemplates(
                name, code, type, active, eventType, pageable);
        return ResponseEntity.ok(templates);
    }

    @GetMapping("/type/{type}")
    @Operation(summary = "Obtener plantillas por tipo", description = "Obtiene plantillas activas por tipo de notificación")
    @PreAuthorize("hasAnyRole('ADMIN', 'NOTIFICATION_ADMIN', 'NOTIFICATION_VIEWER')")
    public ResponseEntity<List<TemplateDto>> getTemplatesByType(
            @PathVariable("type") NotificationType type) {
        log.info("Solicitud para obtener plantillas por tipo: {}", type);
        List<TemplateDto> templates = templateService.findActiveTemplatesByType(type);
        return ResponseEntity.ok(templates);
    }

    @GetMapping("/event-type/{eventType}")
    @Operation(summary = "Obtener plantillas por tipo de evento", description = "Obtiene plantillas activas por tipo de evento")
    @PreAuthorize("hasAnyRole('ADMIN', 'NOTIFICATION_ADMIN', 'NOTIFICATION_VIEWER')")
    public ResponseEntity<List<TemplateDto>> getTemplatesByEventType(
            @PathVariable("eventType") String eventType) {
        log.info("Solicitud para obtener plantillas por tipo de evento: {}", eventType);
        List<TemplateDto> templates = templateService.findActiveTemplatesByEventType(eventType);
        return ResponseEntity.ok(templates);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Activar/desactivar plantilla", description = "Cambia el estado de activación de una plantilla")
    @PreAuthorize("hasAnyRole('ADMIN', 'NOTIFICATION_ADMIN')")
    public ResponseEntity<TemplateDto> toggleTemplateStatus(
            @PathVariable("id") UUID id,
            @RequestParam boolean active) {
        log.info("Solicitud para cambiar estado de plantilla con ID: {} a {}", id, active);
        TemplateDto updatedTemplate = templateService.toggleTemplateStatus(id, active);
        return ResponseEntity.ok(updatedTemplate);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar plantilla", description = "Elimina una plantilla existente")
    @PreAuthorize("hasAnyRole('ADMIN', 'NOTIFICATION_ADMIN')")
    public ResponseEntity<Void> deleteTemplate(
            @PathVariable("id") UUID id) {
        log.info("Solicitud para eliminar plantilla con ID: {}", id);
        boolean deleted = templateService.deleteTemplate(id);

        if (deleted) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/validate")
    @Operation(summary = "Validar variables de plantilla", description = "Valida si las variables proporcionadas satisfacen los requisitos de la plantilla")
    @PreAuthorize("hasAnyRole('ADMIN', 'NOTIFICATION_ADMIN', 'NOTIFICATION_SENDER')")
    public ResponseEntity<Boolean> validateTemplateVariables(
            @RequestParam UUID templateId,
            @RequestBody Map<String, Object> variables) {
        log.info("Solicitud para validar variables para plantilla con ID: {}", templateId);
        boolean valid = templateService.validateTemplateVariables(templateId, variables);
        return ResponseEntity.ok(valid);
    }
}
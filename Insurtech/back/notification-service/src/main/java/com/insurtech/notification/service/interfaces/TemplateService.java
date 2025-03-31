package com.insurtech.notification.service.interfaces;

import com.insurtech.notification.model.dto.TemplateDto;
import com.insurtech.notification.model.entity.NotificationTemplate;
import com.insurtech.notification.model.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface TemplateService {

    /**
     * Crea una nueva plantilla
     *
     * @param templateDto datos de la plantilla
     * @return plantilla creada
     */
    TemplateDto createTemplate(TemplateDto templateDto);

    /**
     * Actualiza una plantilla existente
     *
     * @param id identificador de la plantilla
     * @param templateDto datos actualizados
     * @return plantilla actualizada
     */
    TemplateDto updateTemplate(UUID id, TemplateDto templateDto);

    /**
     * Busca una plantilla por su ID
     *
     * @param id identificador de la plantilla
     * @return plantilla si existe
     */
    Optional<TemplateDto> findTemplateById(UUID id);

    /**
     * Busca una plantilla por su código
     *
     * @param code código de la plantilla
     * @return plantilla si existe
     */
    Optional<NotificationTemplate> findTemplateByCode(String code);

    /**
     * Obtiene todas las plantillas con paginación
     *
     * @param pageable información de paginación
     * @return página de plantillas
     */
    Page<TemplateDto> findAllTemplates(Pageable pageable);

    /**
     * Busca plantillas con filtros
     *
     * @param name nombre (parcial)
     * @param code código (parcial)
     * @param type tipo de notificación
     * @param active estado de activación
     * @param eventType tipo de evento asociado
     * @param pageable información de paginación
     * @return página de plantillas filtradas
     */
    Page<TemplateDto> searchTemplates(
            String name,
            String code,
            NotificationType type,
            Boolean active,
            String eventType,
            Pageable pageable);

    /**
     * Obtiene plantillas activas por tipo
     *
     * @param type tipo de notificación
     * @return lista de plantillas
     */
    List<TemplateDto> findActiveTemplatesByType(NotificationType type);

    /**
     * Obtiene plantillas activas por tipo de evento
     *
     * @param eventType tipo de evento
     * @return lista de plantillas
     */
    List<TemplateDto> findActiveTemplatesByEventType(String eventType);

    /**
     * Procesa una plantilla sustituyendo las variables
     *
     * @param templateContent contenido de la plantilla
     * @param variables variables a sustituir
     * @return contenido procesado
     */
    String processTemplate(String templateContent, Map<String, Object> variables);

    /**
     * Elimina una plantilla
     *
     * @param id identificador de la plantilla
     * @return true si se eliminó correctamente
     */
    boolean deleteTemplate(UUID id);

    /**
     * Activa o desactiva una plantilla
     *
     * @param id identificador de la plantilla
     * @param active nuevo estado
     * @return plantilla actualizada
     */
    TemplateDto toggleTemplateStatus(UUID id, boolean active);

    /**
     * Valida que una plantilla tenga todas las variables requeridas
     *
     * @param templateId identificador de la plantilla
     * @param variables variables proporcionadas
     * @return true si todas las variables requeridas están presentes
     */
    boolean validateTemplateVariables(UUID templateId, Map<String, Object> variables);
}
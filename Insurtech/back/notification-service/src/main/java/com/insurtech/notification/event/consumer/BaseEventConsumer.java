package com.insurtech.notification.event.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.insurtech.notification.event.model.BaseEvent;
import com.insurtech.notification.exception.NotificationException;
import com.insurtech.notification.service.interfaces.NotificationService;
import com.insurtech.notification.service.interfaces.TemplateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.support.Acknowledgment;

@Slf4j
public abstract class BaseEventConsumer<T extends BaseEvent> {

    @Autowired
    protected NotificationService notificationService;

    @Autowired
    protected TemplateService templateService;

    @Autowired
    protected ObjectMapper objectMapper;

    /**
     * Procesa un evento recibido desde Kafka
     *
     * @param event evento a procesar
     * @param acknowledgment callback para reconocer el mensaje
     */
    protected void processEvent(T event, Acknowledgment acknowledgment) {
        try {
            log.info("Procesando evento: {}, tipo: {}, ID: {}",
                    event.getClass().getSimpleName(), event.getEventType(), event.getEventId());

            if (!isValidEvent(event)) {
                log.warn("Evento no válido para procesamiento: {}", event.getEventId());
                acknowledgment.acknowledge();
                return;
            }

            // Procesamiento específico implementado por subclases
            processEventInternal(event);

            acknowledgment.acknowledge();
            log.info("Evento procesado correctamente: {}", event.getEventId());
        } catch (NotificationException e) {
            log.error("Error procesando evento {}: {}", event.getEventId(), e.getMessage());
            acknowledgment.acknowledge(); // Reconocemos incluso en caso de error de negocio
        } catch (Exception e) {
            log.error("Error inesperado procesando evento {}", event.getEventId(), e);
            // No reconocemos el mensaje en caso de error inesperado para que Kafka lo reintente
            // La plataforma Kafka está configurada con reintentos y backoff
        }
    }

    /**
     * Valida si el evento debe ser procesado
     *
     * @param event evento a validar
     * @return true si el evento es válido para procesamiento
     */
    protected boolean isValidEvent(T event) {
        return event != null && event.getEventId() != null && event.getEventType() != null;
    }

    /**
     * Implementación específica para procesar el evento
     *
     * @param event evento a procesar
     * @throws NotificationException en caso de error de negocio
     */
    protected abstract void processEventInternal(T event) throws NotificationException;
}
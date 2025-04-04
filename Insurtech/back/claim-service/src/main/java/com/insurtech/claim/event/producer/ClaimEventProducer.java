package com.insurtech.claim.event.producer;

import com.insurtech.claim.model.entity.Claim;
import com.insurtech.claim.model.entity.ClaimEvent;
import com.insurtech.claim.model.entity.ClaimItem;
import com.insurtech.claim.repository.ClaimEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class ClaimEventProducer {

    private static final Logger log = LoggerFactory.getLogger(ClaimEventProducer.class);

    private static final String CLAIM_CREATED_TOPIC = "claim.created";
    private static final String CLAIM_UPDATED_TOPIC = "claim.updated";
    private static final String CLAIM_STATUS_CHANGED_TOPIC = "claim.status.changed";
    private static final String CLAIM_ITEM_ADDED_TOPIC = "claim.item.added";

    private final ClaimEventRepository claimEventRepository;
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.events.enabled:false}")
    private boolean eventsEnabled;

    @Value("${kafka.enabled:false}")
    private boolean kafkaEnabled;

    @Autowired
    public ClaimEventProducer(
            @Autowired(required = false) KafkaTemplate<String, Object> kafkaTemplate,
            ClaimEventRepository claimEventRepository) {
        this.kafkaTemplate = kafkaTemplate;
        this.claimEventRepository = claimEventRepository;
    }

    public void publishClaimCreated(Claim claim) {
        // Verificar si los eventos están habilitados y Kafka está disponible
        if (!eventsEnabled || !kafkaEnabled || kafkaTemplate == null) {
            log.info("Eventos o Kafka desactivados: se omite la publicación del evento de reclamación creada");
            // Aún así registramos el evento en la base de datos
            persistEvent(claim, ClaimEvent.EventType.CLAIM_CREATED,
                    "Reclamación creada (sin publicar en Kafka)", null, claim.getStatus(), UUID.randomUUID().toString());
            return;
        }

        log.info("Publicando evento de reclamación creada para la reclamación ID: {}", claim.getId());

        String eventId = UUID.randomUUID().toString();

        Map<String, Object> event = new HashMap<>();
        event.put("eventId", eventId);
        event.put("eventType", "CLAIM_CREATED");
        event.put("claimId", claim.getId());
        event.put("claimNumber", claim.getClaimNumber());
        event.put("policyId", claim.getPolicyId());
        event.put("policyNumber", claim.getPolicyNumber());
        event.put("customerId", claim.getCustomerId());
        event.put("customerNumber", claim.getCustomerNumber());
        event.put("status", claim.getStatus().name());
        event.put("claimType", claim.getClaimType() != null ? claim.getClaimType().name() : null);
        event.put("timestamp", System.currentTimeMillis());

        try {
            kafkaTemplate.send(CLAIM_CREATED_TOPIC, claim.getClaimNumber(), event);
            log.debug("Evento de reclamación creada publicado exitosamente");

            // Registrar el evento en la base de datos
            persistEvent(claim, ClaimEvent.EventType.CLAIM_CREATED,
                    "Reclamación creada", null, claim.getStatus(), eventId);
        } catch (Exception e) {
            log.error("Error al publicar evento de reclamación creada - continuando sin interrumpir el flujo", e);
        }
    }

    public void publishClaimUpdated(Claim claim) {
        // Verificar si los eventos están habilitados y Kafka está disponible
        if (!eventsEnabled || !kafkaEnabled || kafkaTemplate == null) {
            log.info("Eventos o Kafka desactivados: se omite la publicación del evento de reclamación actualizada");
            // Aún así registramos el evento en la base de datos
            persistEvent(claim, ClaimEvent.EventType.CLAIM_UPDATED,
                    "Reclamación actualizada (sin publicar en Kafka)", null, null, UUID.randomUUID().toString());
            return;
        }

        log.info("Publicando evento de reclamación actualizada para la reclamación ID: {}", claim.getId());

        String eventId = UUID.randomUUID().toString();

        Map<String, Object> event = new HashMap<>();
        event.put("eventId", eventId);
        event.put("eventType", "CLAIM_UPDATED");
        event.put("claimId", claim.getId());
        event.put("claimNumber", claim.getClaimNumber());
        event.put("policyNumber", claim.getPolicyNumber());
        event.put("customerId", claim.getCustomerId());
        event.put("status", claim.getStatus().name());
        event.put("timestamp", System.currentTimeMillis());
        event.put("updatedBy", claim.getUpdatedBy());

        try {
            kafkaTemplate.send(CLAIM_UPDATED_TOPIC, claim.getClaimNumber(), event);
            log.debug("Evento de reclamación actualizada publicado exitosamente");

            // Registrar el evento en la base de datos
            persistEvent(claim, ClaimEvent.EventType.CLAIM_UPDATED,
                    "Reclamación actualizada", null, null, eventId);
        } catch (Exception e) {
            log.error("Error al publicar evento de reclamación actualizada - continuando sin interrumpir el flujo", e);
        }
    }

    public void publishClaimStatusChanged(Claim claim, Claim.ClaimStatus oldStatus) {
        // Verificar si los eventos están habilitados y Kafka está disponible
        if (!eventsEnabled || !kafkaEnabled || kafkaTemplate == null) {
            log.info("Eventos o Kafka desactivados: se omite la publicación del evento de cambio de estado");
            // Aún así registramos el evento en la base de datos
            persistEvent(claim, ClaimEvent.EventType.STATUS_CHANGED,
                    "Estado cambiado (sin publicar en Kafka)", oldStatus, claim.getStatus(), UUID.randomUUID().toString());
            return;
        }

        log.info("Publicando evento de cambio de estado para la reclamación ID: {}", claim.getId());

        String eventId = UUID.randomUUID().toString();

        Map<String, Object> event = new HashMap<>();
        event.put("eventId", eventId);
        event.put("eventType", "CLAIM_STATUS_CHANGED");
        event.put("claimId", claim.getId());
        event.put("claimNumber", claim.getClaimNumber());
        event.put("policyNumber", claim.getPolicyNumber());
        event.put("customerId", claim.getCustomerId());
        event.put("oldStatus", oldStatus.name());
        event.put("newStatus", claim.getStatus().name());
        event.put("timestamp", System.currentTimeMillis());
        event.put("updatedBy", claim.getUpdatedBy());

        try {
            kafkaTemplate.send(CLAIM_STATUS_CHANGED_TOPIC, claim.getClaimNumber(), event);
            log.debug("Evento de cambio de estado publicado exitosamente");

            // Registrar el evento en la base de datos
            persistEvent(claim, ClaimEvent.EventType.STATUS_CHANGED,
                    "Estado cambiado", oldStatus, claim.getStatus(), eventId);
        } catch (Exception e) {
            log.error("Error al publicar evento de cambio de estado - continuando sin interrumpir el flujo", e);
        }
    }

    public void publishClaimItemAdded(Claim claim, ClaimItem item) {
        // Verificar si los eventos están habilitados y Kafka está disponible
        if (!eventsEnabled || !kafkaEnabled || kafkaTemplate == null) {
            log.info("Eventos o Kafka desactivados: se omite la publicación del evento de ítem añadido");
            // Aún así registramos el evento en la base de datos
            persistEvent(claim, ClaimEvent.EventType.ITEM_ADDED,
                    "Ítem añadido: " + item.getDescription() + " (sin publicar en Kafka)", null, null, UUID.randomUUID().toString());
            return;
        }

        log.info("Publicando evento de ítem añadido para la reclamación ID: {}", claim.getId());

        String eventId = UUID.randomUUID().toString();

        Map<String, Object> event = new HashMap<>();
        event.put("eventId", eventId);
        event.put("eventType", "CLAIM_ITEM_ADDED");
        event.put("claimId", claim.getId());
        event.put("claimNumber", claim.getClaimNumber());
        event.put("itemId", item.getId());
        event.put("itemDescription", item.getDescription());
        event.put("itemAmount", item.getClaimedAmount());
        event.put("timestamp", System.currentTimeMillis());
        event.put("createdBy", item.getCreatedBy());

        try {
            kafkaTemplate.send(CLAIM_ITEM_ADDED_TOPIC, claim.getClaimNumber(), event);
            log.debug("Evento de ítem añadido publicado exitosamente");

            // Registrar el evento en la base de datos
            persistEvent(claim, ClaimEvent.EventType.ITEM_ADDED,
                    "Ítem añadido: " + item.getDescription(), null, null, eventId);
        } catch (Exception e) {
            log.error("Error al publicar evento de ítem añadido - continuando sin interrumpir el flujo", e);
        }
    }

    /**
     * Almacena un evento en la base de datos para auditoría e historial
     */
    private void persistEvent(
            Claim claim,
            ClaimEvent.EventType eventType,
            String details,
            Claim.ClaimStatus oldStatus,
            Claim.ClaimStatus newStatus,
            String eventId) {

        try {
            ClaimEvent claimEvent = new ClaimEvent();
            claimEvent.setClaimId(claim.getId());
            claimEvent.setEventType(eventType);
            claimEvent.setDetails(details);
            claimEvent.setOldStatus(oldStatus);
            claimEvent.setNewStatus(newStatus);
            claimEvent.setEventId(eventId);
            claimEvent.setCreatedBy(claim.getUpdatedBy() != null ? claim.getUpdatedBy() : "system");

            claimEventRepository.save(claimEvent);
        } catch (Exception e) {
            log.error("Error al persistir evento en base de datos", e);
        }
    }
}
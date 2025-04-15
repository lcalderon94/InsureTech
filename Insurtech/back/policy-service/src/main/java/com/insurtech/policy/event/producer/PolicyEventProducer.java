package com.insurtech.policy.event.producer;

import com.insurtech.policy.model.entity.Policy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class PolicyEventProducer {

    private static final Logger log = LoggerFactory.getLogger(PolicyEventProducer.class);

    private static final String POLICY_CREATED_TOPIC = "policy.created";
    private static final String POLICY_UPDATED_TOPIC = "policy.updated";
    private static final String POLICY_STATUS_CHANGED_TOPIC = "policy.status.changed";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    public PolicyEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishPolicyCreated(Policy policy) {
        log.info("Publicando evento de póliza creada para póliza ID: {}", policy.getId());

        Map<String, Object> event = new HashMap<>();
        event.put("eventId", UUID.randomUUID().toString());
        event.put("eventType", "POLICY_CREATED");
        event.put("policyId", policy.getId());
        event.put("policyNumber", policy.getPolicyNumber());
        event.put("customerId", policy.getCustomerId());
        event.put("policyType", policy.getPolicyType().name());
        event.put("status", policy.getStatus().name());
        event.put("timestamp", System.currentTimeMillis());

        try {
            kafkaTemplate.send(POLICY_CREATED_TOPIC, policy.getPolicyNumber(), event);
            log.debug("Evento de póliza creada publicado exitosamente");
        } catch (Exception e) {
            log.error("Error al publicar evento de póliza creada", e);
        }
    }

    public void publishPolicyUpdated(Policy policy) {
        log.info("Publicando evento de póliza actualizada para póliza ID: {}", policy.getId());

        Map<String, Object> event = new HashMap<>();
        event.put("eventId", UUID.randomUUID().toString());
        event.put("eventType", "POLICY_UPDATED");
        event.put("policyId", policy.getId());
        event.put("policyNumber", policy.getPolicyNumber());
        event.put("customerId", policy.getCustomerId());
        event.put("status", policy.getStatus().name());
        event.put("timestamp", System.currentTimeMillis());
        event.put("updatedBy", policy.getUpdatedBy());

        try {
            kafkaTemplate.send(POLICY_UPDATED_TOPIC, policy.getPolicyNumber(), event);
            log.debug("Evento de póliza actualizada publicado exitosamente");
        } catch (Exception e) {
            log.error("Error al publicar evento de póliza actualizada", e);
        }
    }

    public void publishPolicyStatusChanged(Policy policy, Policy.PolicyStatus oldStatus) {
        log.info("Publicando evento de cambio de estado para póliza ID: {}", policy.getId());

        Map<String, Object> event = new HashMap<>();
        event.put("eventId", UUID.randomUUID().toString());
        event.put("eventType", "POLICY_STATUS_CHANGED");
        event.put("policyId", policy.getId());
        event.put("policyNumber", policy.getPolicyNumber());
        event.put("customerId", policy.getCustomerId());
        event.put("oldStatus", oldStatus.name());
        event.put("newStatus", policy.getStatus().name());
        event.put("timestamp", System.currentTimeMillis());
        event.put("updatedBy", policy.getUpdatedBy());

        try {
            kafkaTemplate.send(POLICY_STATUS_CHANGED_TOPIC, policy.getPolicyNumber(), event);
            log.debug("Evento de cambio de estado publicado exitosamente");
        } catch (Exception e) {
            log.error("Error al publicar evento de cambio de estado", e);
        }
    }
}
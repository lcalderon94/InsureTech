package com.insurtech.policy.event.producer;

import com.insurtech.policy.client.CustomerClient;
import com.insurtech.policy.model.dto.PolicyDto;
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

    private final CustomerClient customerClient;


    @Autowired
    public PolicyEventProducer(KafkaTemplate<String, Object> kafkaTemplate, CustomerClient customerClient) {
        this.kafkaTemplate = kafkaTemplate;
        this.customerClient = customerClient;
    }

    public void publishPolicyCreated(PolicyDto policyDto) {
        log.info("Publicando evento de póliza creada para póliza ID: {}", policyDto.getId());

        // Recuperar datos completos del cliente si es necesario
        Map<String, Object> customerData;
        try {
            customerData = customerClient.getCustomerById(policyDto.getCustomerId());
        } catch (Exception e) {
            log.error("Error al recuperar datos del cliente para evento", e);
            customerData = new HashMap<>();
        }

        Map<String, Object> event = new HashMap<>();

        // Metadatos del evento
        event.put("eventId", UUID.randomUUID().toString());
        event.put("eventType", "POLICY_CREATED");
        event.put("timestamp", System.currentTimeMillis());

        // Datos básicos de la póliza
        event.put("policyId", policyDto.getId());
        event.put("policyNumber", policyDto.getPolicyNumber());
        event.put("customerId", policyDto.getCustomerId());
        event.put("policyType", policyDto.getPolicyType().name());
        event.put("status", policyDto.getStatus().name());

        // Datos del cliente (desde DTO y complementado con customerData)
        event.put("customerEmail", policyDto.getCustomerEmail() != null ?
                policyDto.getCustomerEmail() : customerData.getOrDefault("email", null));
        event.put("customerPhone", customerData.getOrDefault("phoneNumber", null));
        event.put("customerName", customerData.getOrDefault("fullName", null));
        event.put("customerNumber", policyDto.getCustomerNumber() != null ?
                policyDto.getCustomerNumber() : customerData.getOrDefault("customerNumber", null));
        event.put("identificationType", policyDto.getIdentificationType() != null ?
                policyDto.getIdentificationType() : customerData.getOrDefault("identificationType", null));
        event.put("identificationNumber", policyDto.getIdentificationNumber() != null ?
                policyDto.getIdentificationNumber() : customerData.getOrDefault("identificationNumber", null));

        // Datos adicionales de la póliza
        event.put("startDate", policyDto.getStartDate() != null ? policyDto.getStartDate().toString() : null);
        event.put("endDate", policyDto.getEndDate() != null ? policyDto.getEndDate().toString() : null);
        event.put("premium", policyDto.getPremium() != null ? policyDto.getPremium().toString() : null);
        event.put("sumInsured", policyDto.getSumInsured() != null ? policyDto.getSumInsured().toString() : null);

        // Información sobre coberturas
        event.put("coveragesCount", policyDto.getCoverages() != null ? policyDto.getCoverages().size() : 0);

        try {
            kafkaTemplate.send(POLICY_CREATED_TOPIC, policyDto.getPolicyNumber(), event);
            log.debug("Evento de póliza creada publicado exitosamente");
        } catch (Exception e) {
            log.error("Error al publicar evento de póliza creada", e);
        }
    }

    public void publishPolicyUpdated(PolicyDto policyDto) {
        log.info("Publicando evento de póliza actualizada para póliza ID: {}", policyDto.getId());

        Map<String, Object> event = new HashMap<>();
        event.put("eventId", UUID.randomUUID().toString());
        event.put("eventType", "POLICY_UPDATED");
        event.put("policyId", policyDto.getId());
        event.put("policyNumber", policyDto.getPolicyNumber());
        event.put("customerId", policyDto.getCustomerId());
        event.put("status", policyDto.getStatus().name());
        event.put("timestamp", System.currentTimeMillis());

        // Agregar datos de cliente para notificaciones
        event.put("customerEmail", policyDto.getCustomerEmail());
        event.put("customerNumber", policyDto.getCustomerNumber());

        try {
            kafkaTemplate.send(POLICY_UPDATED_TOPIC, policyDto.getPolicyNumber(), event);
            log.debug("Evento de póliza actualizada publicado exitosamente");
        } catch (Exception e) {
            log.error("Error al publicar evento de póliza actualizada", e);
        }
    }

    public void publishPolicyStatusChanged(PolicyDto policyDto, Policy.PolicyStatus oldStatus) {
        log.info("Publicando evento de cambio de estado para póliza ID: {}", policyDto.getId());

        Map<String, Object> event = new HashMap<>();
        event.put("eventId", UUID.randomUUID().toString());
        event.put("eventType", "POLICY_STATUS_CHANGED");
        event.put("policyId", policyDto.getId());
        event.put("policyNumber", policyDto.getPolicyNumber());
        event.put("customerId", policyDto.getCustomerId());
        event.put("oldStatus", oldStatus.name());
        event.put("newStatus", policyDto.getStatus().name());
        event.put("timestamp", System.currentTimeMillis());

        // Agregar datos del cliente
        event.put("customerEmail", policyDto.getCustomerEmail());
        event.put("customerNumber", policyDto.getCustomerNumber());
        event.put("identificationType", policyDto.getIdentificationType());
        event.put("identificationNumber", policyDto.getIdentificationNumber());

        try {
            kafkaTemplate.send(POLICY_STATUS_CHANGED_TOPIC, policyDto.getPolicyNumber(), event);
            log.debug("Evento de cambio de estado publicado exitosamente");
        } catch (Exception e) {
            log.error("Error al publicar evento de cambio de estado", e);
        }
    }
}
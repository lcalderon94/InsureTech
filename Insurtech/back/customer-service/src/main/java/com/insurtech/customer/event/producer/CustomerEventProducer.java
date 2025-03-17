package com.insurtech.customer.event.producer;

import com.insurtech.customer.model.entity.Customer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Servicio para publicar eventos relacionados con clientes en Kafka
 */
@Service
public class CustomerEventProducer {

    private static final Logger log = LoggerFactory.getLogger(CustomerEventProducer.class);

    private static final String CUSTOMER_CREATED_TOPIC = "customer-created";
    private static final String CUSTOMER_UPDATED_TOPIC = "customer-updated";
    private static final String CUSTOMER_STATUS_CHANGED_TOPIC = "customer-status-changed";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    public CustomerEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Publica evento cuando se crea un nuevo cliente
     */
    public void publishCustomerCreated(Customer customer) {
        log.info("Publicando evento de cliente creado para el cliente ID: {}", customer.getId());

        Map<String, Object> event = new HashMap<>();
        event.put("eventId", UUID.randomUUID().toString());
        event.put("eventType", "CUSTOMER_CREATED");
        event.put("customerId", customer.getId());
        event.put("customerNumber", customer.getCustomerNumber());
        event.put("email", customer.getEmail());
        event.put("firstName", customer.getFirstName());
        event.put("lastName", customer.getLastName());
        event.put("timestamp", System.currentTimeMillis());

        try {
            kafkaTemplate.send(CUSTOMER_CREATED_TOPIC, customer.getCustomerNumber(), event);
            log.debug("Evento de cliente creado publicado exitosamente");
        } catch (Exception e) {
            log.error("Error al publicar evento de cliente creado", e);
        }
    }

    /**
     * Publica evento cuando se actualiza un cliente
     */
    public void publishCustomerUpdated(Customer customer) {
        log.info("Publicando evento de cliente actualizado para el cliente ID: {}", customer.getId());

        Map<String, Object> event = new HashMap<>();
        event.put("eventId", UUID.randomUUID().toString());
        event.put("eventType", "CUSTOMER_UPDATED");
        event.put("customerId", customer.getId());
        event.put("customerNumber", customer.getCustomerNumber());
        event.put("email", customer.getEmail());
        event.put("timestamp", System.currentTimeMillis());
        event.put("updatedBy", customer.getUpdatedBy());

        try {
            kafkaTemplate.send(CUSTOMER_UPDATED_TOPIC, customer.getCustomerNumber(), event);
            log.debug("Evento de cliente actualizado publicado exitosamente");
        } catch (Exception e) {
            log.error("Error al publicar evento de cliente actualizado", e);
        }
    }

    /**
     * Publica evento cuando cambia el estado de un cliente
     */
    public void publishCustomerStatusChanged(Customer customer, Customer.CustomerStatus oldStatus) {
        log.info("Publicando evento de cambio de estado para el cliente ID: {}", customer.getId());

        Map<String, Object> event = new HashMap<>();
        event.put("eventId", UUID.randomUUID().toString());
        event.put("eventType", "CUSTOMER_STATUS_CHANGED");
        event.put("customerId", customer.getId());
        event.put("customerNumber", customer.getCustomerNumber());
        event.put("oldStatus", oldStatus.name());
        event.put("newStatus", customer.getStatus().name());
        event.put("timestamp", System.currentTimeMillis());
        event.put("updatedBy", customer.getUpdatedBy());

        try {
            kafkaTemplate.send(CUSTOMER_STATUS_CHANGED_TOPIC, customer.getCustomerNumber(), event);
            log.debug("Evento de cambio de estado publicado exitosamente");
        } catch (Exception e) {
            log.error("Error al publicar evento de cambio de estado", e);
        }
    }
}
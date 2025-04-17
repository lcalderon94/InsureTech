package com.insurtech.notification.event.consumer;

import com.insurtech.notification.model.dto.NotificationRequestDto;
import com.insurtech.notification.model.enums.NotificationPriority;
import com.insurtech.notification.model.enums.NotificationType;
import com.insurtech.notification.service.interfaces.NotificationService;
import com.insurtech.notification.service.interfaces.TemplateService;
import com.insurtech.notification.util.SmsTemplates;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.Year;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class CustomerEventConsumer {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private TemplateService templateService;

    @KafkaListener(topics = "customer.created", containerFactory = "customerKafkaListenerContainerFactory")
    public void consumeCustomerCreated(Map<String, Object> eventMap, Acknowledgment acknowledgment) {
        log.info("Recibido evento de cliente creado: {}", eventMap);
        processEvent(eventMap, acknowledgment);
    }

    @KafkaListener(topics = "customer.updated", containerFactory = "customerKafkaListenerContainerFactory")
    public void consumeCustomerUpdated(Map<String, Object> eventMap, Acknowledgment acknowledgment) {
        log.info("Recibido evento de cliente actualizado: {}", eventMap);
        processEvent(eventMap, acknowledgment);
    }

    @KafkaListener(topics = "customer.status.changed", containerFactory = "customerKafkaListenerContainerFactory")
    public void consumeCustomerStatusChanged(Map<String, Object> eventMap, Acknowledgment acknowledgment) {
        log.info("Recibido evento de cambio de estado de cliente: {}", eventMap);
        processEvent(eventMap, acknowledgment);
    }

    @KafkaListener(topics = "customer.password.reset", containerFactory = "customerKafkaListenerContainerFactory")
    public void consumePasswordReset(Map<String, Object> eventMap, Acknowledgment acknowledgment) {
        log.info("Recibido evento de restablecimiento de contraseña: {}", eventMap);
        processEvent(eventMap, acknowledgment);
    }

    @KafkaListener(topics = "customer.security.alert", containerFactory = "customerKafkaListenerContainerFactory")
    public void consumeSecurityAlert(Map<String, Object> eventMap, Acknowledgment acknowledgment) {
        log.info("Recibido evento de alerta de seguridad: {}", eventMap);
        processEvent(eventMap, acknowledgment);
    }

    protected void processEvent(Map<String, Object> eventMap, Acknowledgment acknowledgment) {
        try {
            // 1. Determinar el tipo de evento
            String eventType = determineEventType(eventMap);
            log.info("Procesando evento: {} con ID: {}", eventType, eventMap.get("eventId"));

            // 2. Extraer variables para plantillas
            Map<String, Object> variables = extractVariables(eventMap);

            // 3. Extraer datos de contacto
            String email = extractEmail(eventMap);
            String phone = extractPhone(eventMap);

            // 4. ID del evento para tracking
            String sourceEventId = eventMap.get("eventId") != null ?
                    eventMap.get("eventId").toString() : UUID.randomUUID().toString();

            // 5. Enviar notificaciones por canales disponibles
            processNotificationsForAllChannels(eventType, email, phone, variables, sourceEventId);

            acknowledgment.acknowledge();
            log.info("Evento procesado correctamente: {}", sourceEventId);
        } catch (Exception e) {
            log.error("Error procesando evento {}: {}", eventMap.get("eventId"), e.getMessage(), e);
            acknowledgment.acknowledge(); // Para evitar mensajes "poison pill"
        }
    }

    protected String determineEventType(Map<String, Object> eventMap) {
        // Si viene explícitamente el tipo de evento
        if (eventMap.get("eventType") != null) {
            String rawType = eventMap.get("eventType").toString();

            // Normalizar formato: CUSTOMER_CREATED -> customer.created
            if (rawType.contains("_")) {
                return rawType.toLowerCase().replace("_", ".");
            }

            return rawType.toLowerCase();
        }

        // Determinar por acción o estado
        if (eventMap.get("actionType") != null) {
            String action = eventMap.get("actionType").toString();
            switch (action.toUpperCase()) {
                case "CREATED": return "customer.created";
                case "UPDATED": return "customer.updated";
                case "PASSWORD_RESET": return "customer.password.reset";
                case "LOGIN_ATTEMPT": return "customer.security.alert";
                case "VERIFIED":
                case "DEACTIVATED":
                    return "customer.status.changed";
            }
        }

        // Si hay cambio de estado
        if (eventMap.get("customerStatus") != null) {
            return "customer.status.changed";
        }

        // Por el tópico de Kafka (si todo lo demás falla)
        return "customer.created";
    }

    protected Map<String, Object> extractVariables(Map<String, Object> eventMap) {
        Map<String, Object> variables = new HashMap<>(eventMap);

        // Datos básicos
        String firstName = getString(eventMap, "firstName");
        String lastName = getString(eventMap, "lastName");

        if (firstName != null && lastName != null) {
            variables.put("customerName", firstName + " " + lastName);
        }

        variables.put("firstName", firstName);
        variables.put("lastName", lastName);
        variables.put("documentNumber", getString(eventMap, "documentNumber"));
        variables.put("email", getString(eventMap, "email"));
        variables.put("phone", getString(eventMap, "phone"));

        // Datos de dirección (si están disponibles)
        extractAddressInfo(eventMap, variables);

        // Estado del cliente
        variables.put("status", getString(eventMap, "customerStatus"));

        // Acción realizada
        variables.put("actionType", getString(eventMap, "actionType"));

        // Fecha de registro para nuevos clientes
        if ("CREATED".equals(getString(eventMap, "actionType"))) {
            variables.put("registrationDate", formatDate(java.time.LocalDate.now()));
            if (eventMap.get("customerId") != null) {
                variables.put("customerId", eventMap.get("customerId").toString());
            }
            variables.put("username", getString(eventMap, "email")); // Por defecto el email
        }

        // Datos para servicios de contacto (muchas plantillas los usan)
        variables.put("customerServicePhone", "900123456");
        variables.put("customerServiceEmail", "atencionalcliente@insurtech.com");

        // Año actual (usado en muchas plantillas)
        variables.put("currentYear", Year.now().getValue());

        return variables;
    }

    private void extractAddressInfo(Map<String, Object> eventMap, Map<String, Object> variables) {
        // Intenta extraer dirección si está directamente en el mapa
        variables.put("address", getString(eventMap, "address"));
        variables.put("city", getString(eventMap, "city"));
        variables.put("zipCode", getString(eventMap, "zipCode"));
        variables.put("country", getString(eventMap, "country"));

        // Si hay un array de direcciones, tomar la primera o marcada como primaria
        try {
            if (eventMap.get("addresses") instanceof List) {
                List<?> addresses = (List<?>) eventMap.get("addresses");
                if (!addresses.isEmpty() && addresses.get(0) instanceof Map) {
                    Map<?, ?> address = (Map<?, ?>) addresses.get(0);

                    if (variables.get("address") == null) {
                        variables.put("address", address.get("street") + " " + address.get("number"));
                    }
                    if (variables.get("city") == null) {
                        variables.put("city", address.get("city"));
                    }
                    if (variables.get("zipCode") == null) {
                        variables.put("zipCode", address.get("postalCode"));
                    }
                    if (variables.get("country") == null) {
                        variables.put("country", address.get("country"));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Error extrayendo información de dirección: {}", e.getMessage());
        }
    }

    protected String extractEmail(Map<String, Object> eventMap) {
        if (eventMap.get("email") != null) {
            return eventMap.get("email").toString();
        }

        try {
            if (eventMap.get("contactMethods") instanceof List) {
                List<?> contacts = (List<?>) eventMap.get("contactMethods");
                for (Object contact : contacts) {
                    if (contact instanceof Map) {
                        Map<?, ?> contactMap = (Map<?, ?>) contact;
                        if ("EMAIL".equals(contactMap.get("contactType"))) {
                            return contactMap.get("contactValue").toString();
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Error extrayendo email de contactMethods: {}", e.getMessage());
        }

        return null;
    }

    protected String extractPhone(Map<String, Object> eventMap) {
        if (eventMap.get("phone") != null) {
            return eventMap.get("phone").toString();
        }

        try {
            if (eventMap.get("contactMethods") instanceof List) {
                List<?> contacts = (List<?>) eventMap.get("contactMethods");
                for (Object contact : contacts) {
                    if (contact instanceof Map) {
                        Map<?, ?> contactMap = (Map<?, ?>) contact;
                        if ("MOBILE_PHONE".equals(contactMap.get("contactType"))) {
                            return contactMap.get("contactValue").toString();
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Error extrayendo teléfono de contactMethods: {}", e.getMessage());
        }

        return null;
    }

    protected void processNotificationsForAllChannels(
            String eventType,
            String email,
            String phone,
            Map<String, Object> variables,
            String sourceEventId) {

        // Si hay email, enviar por email
        if (email != null && !email.trim().isEmpty()) {
            try {
                processEmailNotification(eventType, email, variables, sourceEventId);
            } catch (Exception e) {
                log.error("Error enviando email para evento {}: {}", eventType, e.getMessage());
            }
        } else {
            log.warn("No hay dirección de email disponible para notificación: {}", eventType);
        }

        // Si hay teléfono, enviar también por SMS
        if (phone != null && !phone.trim().isEmpty()) {
            try {
                processSmsNotification(eventType, phone, variables, sourceEventId);
            } catch (Exception e) {
                log.error("Error enviando SMS para evento {}: {}", eventType, e.getMessage());
            }
        } else {
            log.warn("No hay número de teléfono disponible para notificación: {}", eventType);
        }
    }

    protected void processEmailNotification(String eventType, String email, Map<String, Object> variables, String sourceEventId) {
        try {
            // Mapear tipo de evento a código de plantilla
            String templateCode = mapEventTypeToEmailTemplate(eventType);
            log.info("Usando plantilla de email: {} para evento: {}", templateCode, eventType);

            // Crear solicitud de notificación Email
            NotificationRequestDto emailRequest = NotificationRequestDto.builder()
                    .type(NotificationType.EMAIL)
                    .recipient(email)
                    .templateCode(templateCode)
                    .templateVariables(variables)
                    .priority(determinePriority(eventType))
                    .sourceEventId(sourceEventId)
                    .sourceEventType(eventType)
                    .build();

            // Enviar la notificación
            notificationService.createNotification(emailRequest);
            log.info("Email enviado para evento {} a {}", eventType, email);
        } catch (Exception e) {
            log.error("Error enviando Email para evento {}: {}", eventType, e.getMessage(), e);
            throw new RuntimeException("Error enviando Email: " + e.getMessage(), e);
        }
    }

    protected void processSmsNotification(String eventType, String phoneNumber, Map<String, Object> variables, String sourceEventId) {
        try {
            // Obtener y procesar la plantilla SMS
            String smsTemplate = SmsTemplates.getTemplateForEvent(eventType);

            if (smsTemplate == null) {
                log.warn("No se encontró plantilla SMS para evento: {}", eventType);
                return;
            }

            String smsContent = SmsTemplates.processTemplate(smsTemplate, variables);
            log.info("Procesada plantilla SMS para evento: {}", eventType);

            // Crear solicitud de notificación SMS
            NotificationRequestDto smsRequest = NotificationRequestDto.builder()
                    .type(NotificationType.SMS)
                    .recipient(phoneNumber)
                    .content(smsContent)
                    .priority(determinePriority(eventType))
                    .sourceEventId(sourceEventId)
                    .sourceEventType(eventType)
                    .build();

            // Enviar la notificación
            notificationService.createNotification(smsRequest);
            log.info("SMS enviado para evento {} a {}", eventType, phoneNumber);
        } catch (Exception e) {
            log.error("Error enviando SMS para evento {}: {}", eventType, e.getMessage(), e);
            throw new RuntimeException("Error enviando SMS: " + e.getMessage(), e);
        }
    }

    protected NotificationPriority determinePriority(String eventType) {
        // Eventos de alta prioridad
        if (eventType.contains("password.reset") ||
                eventType.contains("security.alert") ||
                eventType.contains("status.changed")) {
            return NotificationPriority.HIGH;
        }

        // Eventos de prioridad media
        if (eventType.contains("created")) {
            return NotificationPriority.MEDIUM;
        }

        // Por defecto
        return NotificationPriority.LOW;
    }

    protected String mapEventTypeToEmailTemplate(String eventType) {
        // Convertir customer.created a customer-created
        return eventType.replace(".", "-");
    }

    // Métodos auxiliares

    private String getString(Map<String, Object> map, String key) {
        return map.get(key) != null ? map.get(key).toString() : null;
    }

    private String formatDate(java.time.LocalDate date) {
        if (date == null) {
            return "";
        }
        return date.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }
}
package com.insurtech.notification.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.insurtech.notification.exception.DeliveryFailedException;
import com.insurtech.notification.model.entity.Notification;
import com.insurtech.notification.model.enums.DeliveryStatus;
import com.insurtech.notification.service.interfaces.SmsService;
import com.insurtech.notification.service.interfaces.TemplateService;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmsServiceImpl implements SmsService {

    private final TemplateService templateService;
    private final ObjectMapper objectMapper;
    private final Map<String, String> smsTemplates;

    @Value("${notification.sms.enabled:true}")
    private boolean smsEnabled;

    @Value("${notification.sms.account-sid}")
    private String accountSid;

    @Value("${notification.sms.auth-token}")
    private String authToken;

    @Value("${notification.sms.from-number}")
    private String fromNumber;

    @PostConstruct
    public void initTwilio() {
        if (smsEnabled) {
            Twilio.init(accountSid, authToken);
            log.info("Servicio Twilio inicializado con cuenta: {}", accountSid);
        } else {
            log.warn("Servicio SMS deshabilitado");
        }
    }

    @Override
    public DeliveryStatus sendSms(Notification notification) {
        log.info("Enviando SMS a: {}", notification.getRecipient());

        if (!smsEnabled) {
            log.warn("Servicio SMS deshabilitado. SMS no enviado a: {}", notification.getRecipient());
            return DeliveryStatus.FAILED;
        }

        try {
            return sendMessage(notification.getRecipient(), notification.getContent());
        } catch (Exception e) {
            log.error("Error enviando SMS: {}", e.getMessage(), e);
            throw new DeliveryFailedException("Error enviando SMS: " + e.getMessage(), e);
        }
    }

    @Override
    public DeliveryStatus sendTemplatedSms(String phoneNumber, String templateCode,
                                           Map<String, Object> templateVariables) {
        log.info("Enviando SMS con plantilla {} a: {}", templateCode, phoneNumber);

        if (!smsEnabled) {
            log.warn("Servicio SMS deshabilitado. SMS no enviado a: {}", phoneNumber);
            return DeliveryStatus.FAILED;
        }

        try {
            // Buscar plantilla
            String templateContent = smsTemplates.get(templateCode);
            if (templateContent == null) {
                log.warn("Plantilla SMS no encontrada: {}", templateCode);
                throw new DeliveryFailedException("Plantilla SMS no encontrada: " + templateCode);
            }

            // Procesar plantilla
            String processedContent = templateService.processTemplate(templateContent, templateVariables);

            // Enviar SMS
            return sendMessage(phoneNumber, processedContent);
        } catch (Exception e) {
            log.error("Error enviando SMS con plantilla: {}", e.getMessage(), e);
            throw new DeliveryFailedException("Error enviando SMS con plantilla: " + e.getMessage(), e);
        }
    }

    @Override
    public DeliveryStatus sendMessage(String phoneNumber, String message) {
        if (!smsEnabled) {
            log.warn("Servicio SMS deshabilitado. SMS no enviado a: {}", phoneNumber);
            return DeliveryStatus.FAILED;
        }

        // Validar número de teléfono
        if (!isValidPhoneNumber(phoneNumber)) {
            log.warn("Número de teléfono inválido: {}", phoneNumber);
            throw new DeliveryFailedException("Número de teléfono inválido: " + phoneNumber);
        }

        try {
            // Formatear número si es necesario
            String formattedNumber = formatPhoneNumber(phoneNumber);

            // Crear mensaje
            Message twilioMessage = Message.creator(
                    new PhoneNumber(formattedNumber),
                    new PhoneNumber(fromNumber),
                    message).create();

            log.info("SMS enviado, SID: {}, Estado: {}",
                    twilioMessage.getSid(), twilioMessage.getStatus());

            // Mapear estado de Twilio a nuestro enum
            return mapTwilioStatus(twilioMessage.getStatus().toString());
        } catch (Exception e) {
            log.error("Error enviando SMS: {}", e.getMessage(), e);
            throw new DeliveryFailedException("Error enviando SMS: " + e.getMessage(), e);
        }
    }

    @Override
    @Async("smsTaskExecutor")
    public CompletableFuture<DeliveryStatus> sendSmsAsync(Notification notification) {
        DeliveryStatus status = sendSms(notification);
        return CompletableFuture.completedFuture(status);
    }

    /**
     * Extrae las variables de contexto de una notificación
     */
    private Map<String, Object> extractTemplateVariables(Notification notification) {
        if (notification.getDataContext() == null || notification.getDataContext().isBlank()) {
            return new HashMap<>();
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> variables = objectMapper.readValue(
                    notification.getDataContext(), Map.class);
            return variables;
        } catch (JsonProcessingException e) {
            log.warn("Error deserializando variables de contexto: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * Valida formato básico de número de teléfono
     */
    private boolean isValidPhoneNumber(String phoneNumber) {
        return phoneNumber != null &&
                phoneNumber.matches("^\\+?[0-9]{8,15}$");
    }

    /**
     * Asegura que el número tenga formato E.164
     */
    private String formatPhoneNumber(String phoneNumber) {
        if (phoneNumber.startsWith("+")) {
            return phoneNumber;
        }
        return "+" + phoneNumber;
    }

    /**
     * Mapea estados de Twilio a nuestro enum DeliveryStatus
     */
    private DeliveryStatus mapTwilioStatus(String twilioStatus) {
        switch (twilioStatus.toUpperCase()) {
            case "QUEUED":
                return DeliveryStatus.QUEUED;
            case "SENDING":
            case "SENT":
                return DeliveryStatus.SENT;
            case "DELIVERED":
                return DeliveryStatus.DELIVERED;
            case "UNDELIVERED":
                return DeliveryStatus.UNDELIVERED;
            case "FAILED":
                return DeliveryStatus.FAILED;
            default:
                return DeliveryStatus.SENT; // Default optimista
        }
    }
}
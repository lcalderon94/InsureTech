package com.insurtech.notification.service.interfaces;

import com.insurtech.notification.model.entity.Notification;
import com.insurtech.notification.model.enums.DeliveryStatus;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface SmsService {

    /**
     * Envía un SMS basado en una notificación
     *
     * @param notification notificación con los datos del SMS
     * @return estado de entrega del SMS
     */
    DeliveryStatus sendSms(Notification notification);

    /**
     * Envía un SMS basado en una plantilla
     *
     * @param phoneNumber número de teléfono destinatario
     * @param templateCode código de la plantilla
     * @param templateVariables variables para la plantilla
     * @return estado de entrega del SMS
     */
    DeliveryStatus sendTemplatedSms(
            String phoneNumber,
            String templateCode,
            Map<String, Object> templateVariables);

    /**
     * Envía un SMS con contenido directo
     *
     * @param phoneNumber número de teléfono destinatario
     * @param message mensaje de texto
     * @return estado de entrega del SMS
     */
    DeliveryStatus sendMessage(String phoneNumber, String message);

    /**
     * Envía un SMS de manera asíncrona
     *
     * @param notification notificación con los datos del SMS
     * @return future con el estado de entrega
     */
    CompletableFuture<DeliveryStatus> sendSmsAsync(Notification notification);
}
package com.insurtech.notification.service.interfaces;

import com.insurtech.notification.model.entity.Notification;
import com.insurtech.notification.model.enums.DeliveryStatus;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface EmailService {

    /**
     * Envía un correo electrónico basado en una notificación
     *
     * @param notification notificación con los datos del correo
     * @return estado de entrega del correo
     */
    DeliveryStatus sendEmail(Notification notification);

    /**
     * Envía un correo electrónico basado en una plantilla
     *
     * @param to destinatario
     * @param cc copias (puede ser null)
     * @param subject asunto
     * @param templateName nombre de la plantilla
     * @param templateVariables variables para la plantilla
     * @return estado de entrega del correo
     */
    DeliveryStatus sendTemplatedEmail(
            String to,
            String[] cc,
            String subject,
            String templateName,
            Map<String, Object> templateVariables);

    /**
     * Envía un correo electrónico con contenido directo
     *
     * @param to destinatario
     * @param cc copias (puede ser null)
     * @param subject asunto
     * @param content contenido (HTML)
     * @return estado de entrega del correo
     */
    DeliveryStatus sendHtmlEmail(
            String to,
            String[] cc,
            String subject,
            String content);

    /**
     * Envía un correo electrónico de manera asíncrona
     *
     * @param notification notificación con los datos del correo
     * @return future con el estado de entrega
     */
    CompletableFuture<DeliveryStatus> sendEmailAsync(Notification notification);
}
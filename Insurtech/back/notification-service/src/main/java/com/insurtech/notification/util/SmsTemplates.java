package com.insurtech.notification.util;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Clase de utilidad que contiene todas las plantillas de SMS
 * y el mapeo entre eventos y plantillas
 */
public class SmsTemplates {

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");

    // Plantillas para Policy Service
    public static final String POLICY_CREATED =
            "InsureTech: Su póliza ${policyNumber} ha sido activada. Cobertura: del ${effectiveDate} al ${expirationDate}. Prima: ${premium}€. Más info en su área personal.";

    public static final String POLICY_STATUS_CHANGED =
            "InsureTech: Su póliza ${policyNumber} ha cambiado a estado ${newStatus}. ${statusChangeImpact} Más información: 900123456";

    public static final String POLICY_EXPIRING_SOON =
            "InsureTech: Su póliza ${policyNumber} vence en ${daysUntilExpiration} días. Renueve antes del ${expirationDate} para mantener su protección. Info: 900123456";

    public static final String POLICY_RENEWED =
            "InsureTech: Su póliza ${policyNumber} ha sido renovada correctamente. Nueva validez: del ${newEffectiveDate} al ${newExpirationDate}. Prima: ${newPremium}€";

    public static final String POLICY_CANCELLED =
            "InsureTech: Su póliza ${policyNumber} ha sido cancelada, efectivo desde ${cancellationDate}. Su cobertura finaliza el ${coverageEndDate}. ${refundInfoShort}";

    // Plantillas para Claim Service
    public static final String CLAIM_CREATED =
            "InsureTech: Reclamación ${claimNumber} recibida. Ref: ${claimReference}. Seguimiento en https://insrtech.co/c/${claimId}";

    public static final String CLAIM_STATUS_CHANGED =
            "InsureTech: Su reclamación ${claimNumber} ahora está en estado \"${newStatus}\". Detalles en su área personal o llame al 900123456.";

    public static final String CLAIM_APPROVED =
            "InsureTech: ¡APROBADA! Su reclamación ${claimNumber} ha sido aprobada por ${approvedAmount}€. El pago se procesará en ${paymentDays} días hábiles.";

    public static final String CLAIM_REJECTED =
            "InsureTech: Resolución: Su reclamación ${claimNumber} no ha sido aprobada. Motivo: ${rejectionReasonShort}. Para más información acceda a su área personal.";

    public static final String CLAIM_DOCUMENT_REQUIRED =
            "InsureTech: Se requiere documentación adicional para su reclamación ${claimNumber}. Por favor, revise su email o área personal antes del ${deadlineDate}.";

    // Plantillas para Payment Service
    public static final String PAYMENT_REMINDER =
            "InsureTech: RECORDATORIO: Pago de ${amountDue}€ para su póliza ${policyNumber} vence el ${dueDate}. Pague online: https://insrtech.co/p/${paymentId}";

    public static final String PAYMENT_PROCESSED =
            "InsureTech: CONFIRMADO pago de ${paymentAmount}€ por póliza ${policyNumber}. Ref: ${paymentReference}. Gracias por su confianza.";

    public static final String PAYMENT_FAILED =
            "InsureTech: ALERTA: Pago rechazado para póliza ${policyNumber}. Por favor regularice su situación antes del ${gracePeriodDate} para mantener su cobertura.";

    public static final String PAYMENT_REFUND =
            "InsureTech: Reembolso de ${refundAmount}€ procesado para su póliza ${policyNumber}. Disponible en su cuenta en ${refundTimeframeDays} días hábiles.";

    // Plantillas para Customer Service
    public static final String CUSTOMER_CREATED =
            "InsureTech: ¡Bienvenido! Su cuenta ha sido creada correctamente. Acceda a https://insrtech.co/login para completar su perfil y explorar nuestros servicios.";

    public static final String PASSWORD_RESET =
            "InsureTech: Código de verificación para restablecer contraseña: ${resetCode}. Válido por ${expiryMinutes} minutos. No lo comparta con nadie.";

    public static final String ACCOUNT_SECURITY_ALERT =
            "InsureTech SEGURIDAD: Actividad inusual detectada en su cuenta. Si no reconoce el acceso desde ${location}, cambie su contraseña inmediatamente.";

    // Mapa que relaciona tópicos Kafka con las plantillas correspondientes
    private static final Map<String, String> EVENT_TEMPLATE_MAPPING = new HashMap<>();

    // Inicialización del mapeo entre eventos y plantillas
    static {
        // Mapeo para Policy Service
        EVENT_TEMPLATE_MAPPING.put("policy.created", POLICY_CREATED);
        EVENT_TEMPLATE_MAPPING.put("policy.status.changed", POLICY_STATUS_CHANGED);
        EVENT_TEMPLATE_MAPPING.put("policy.expiring_soon", POLICY_EXPIRING_SOON);
        EVENT_TEMPLATE_MAPPING.put("policy.renewed", POLICY_RENEWED);
        EVENT_TEMPLATE_MAPPING.put("policy.cancelled", POLICY_CANCELLED);

        // Mapeo para Claim Service
        EVENT_TEMPLATE_MAPPING.put("claim.created", CLAIM_CREATED);
        EVENT_TEMPLATE_MAPPING.put("claim.status.changed", CLAIM_STATUS_CHANGED);
        EVENT_TEMPLATE_MAPPING.put("claim.approved", CLAIM_APPROVED);
        EVENT_TEMPLATE_MAPPING.put("claim.rejected", CLAIM_REJECTED);
        EVENT_TEMPLATE_MAPPING.put("claim.document.required", CLAIM_DOCUMENT_REQUIRED);

        // Mapeo para Payment Service
        EVENT_TEMPLATE_MAPPING.put("payment.reminder", PAYMENT_REMINDER);
        EVENT_TEMPLATE_MAPPING.put("payment.processed", PAYMENT_PROCESSED);
        EVENT_TEMPLATE_MAPPING.put("payment.failed", PAYMENT_FAILED);
        EVENT_TEMPLATE_MAPPING.put("payment.refund.processed", PAYMENT_REFUND);

        // Mapeo para Customer Service
        EVENT_TEMPLATE_MAPPING.put("customer.created", CUSTOMER_CREATED);
        EVENT_TEMPLATE_MAPPING.put("customer.password.reset", PASSWORD_RESET);
        EVENT_TEMPLATE_MAPPING.put("customer.security.alert", ACCOUNT_SECURITY_ALERT);
    }

    /**
     * Obtiene la plantilla correspondiente para un evento
     *
     * @param eventType tipo de evento (ej: "policy.created")
     * @return plantilla correspondiente o null si no existe
     */
    public static String getTemplateForEvent(String eventType) {
        return EVENT_TEMPLATE_MAPPING.get(eventType);
    }

    /**
     * Procesa una plantilla sustituyendo sus variables
     *
     * @param template plantilla a procesar
     * @param variables mapa de variables a sustituir
     * @return mensaje procesado
     */
    public static String processTemplate(String template, Map<String, Object> variables) {
        if (template == null || variables == null) {
            return template;
        }

        String result = template;
        Matcher matcher = VARIABLE_PATTERN.matcher(template);

        while (matcher.find()) {
            String variableName = matcher.group(1);
            Object value = variables.get(variableName);
            String replacement = (value != null) ? value.toString() : "";

            result = result.replace("${" + variableName + "}", replacement);
        }

        return result;
    }

    /**
     * Procesa una plantilla para un tipo de evento específico
     *
     * @param eventType tipo de evento
     * @param variables variables para sustituir
     * @return mensaje procesado
     */
    public static String processEventTemplate(String eventType, Map<String, Object> variables) {
        String template = getTemplateForEvent(eventType);
        if (template == null) {
            throw new IllegalArgumentException("No existe plantilla para el evento: " + eventType);
        }

        return processTemplate(template, variables);
    }
}
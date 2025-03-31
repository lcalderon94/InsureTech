package com.insurtech.notification.config;

import com.twilio.Twilio;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class SmsConfig {

    @Value("${notification.sms.enabled}")
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
        }
    }

    @Bean
    public Map<String, String> smsTemplates() {
        Map<String, String> templates = new HashMap<>();

        try {
            // Cargar plantillas desde archivos
            loadTemplateFromFile(templates, "payment-confirmation", "templates/sms/payment-confirmation.txt");
            loadTemplateFromFile(templates, "claim-status-update", "templates/sms/claim-status-update.txt");
            loadTemplateFromFile(templates, "policy-renewal", "templates/sms/policy-renewal.txt");
        } catch (IOException e) {
            // Usar plantillas en memoria como respaldo
            templates.put("payment-confirmation", "Su pago de ${amount} ${currency} ha sido confirmado para la póliza ${policyNumber}. Ref: ${paymentReference}");
            templates.put("claim-status-update", "Su reclamación ${claimNumber} ha cambiado a estado: ${claimStatus}. Más información en su portal de cliente.");
            templates.put("policy-renewal", "Su póliza ${policyNumber} vence el ${expirationDate}. Renueve pronto para mantener su cobertura.");
        }

        return templates;
    }

    private void loadTemplateFromFile(Map<String, String> templates, String templateName, String path) throws IOException {
        Resource resource = new ClassPathResource(path);
        if (resource.exists()) {
            String content = new String(Files.readAllBytes(Paths.get(resource.getURI())));
            templates.put(templateName, content);
        }
    }
}
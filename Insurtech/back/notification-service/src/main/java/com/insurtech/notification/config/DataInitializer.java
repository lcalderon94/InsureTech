package com.insurtech.notification.config;

import com.insurtech.notification.model.dto.TemplateDto;
import com.insurtech.notification.model.enums.NotificationType;
import com.insurtech.notification.service.interfaces.TemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Inicializador para crear datos predeterminados en la base de datos
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final TemplateService templateService;

    @Override
    public void run(String... args) throws Exception {
        log.info("Inicializando datos predeterminados");
        initDefaultTemplates();
    }

    /**
     * Crea plantillas predeterminadas si no existen
     */
    private void initDefaultTemplates() {
        log.info("Inicializando plantillas predeterminadas");
        List<TemplateDto> defaultTemplates = createDefaultTemplates();

        for (TemplateDto template : defaultTemplates) {
            try {
                // Verificar si la plantilla ya existe
                Optional<TemplateDto> existingTemplate = templateService.findTemplateByCode(template.getCode())
                        .map(t -> {
                            TemplateDto dto = new TemplateDto();
                            dto.setId(t.getId());
                            dto.setCode(t.getCode());
                            return dto;
                        });

                if (existingTemplate.isEmpty()) {
                    log.info("Creando plantilla predeterminada: {}", template.getCode());
                    templateService.createTemplate(template);
                } else {
                    log.info("La plantilla {} ya existe", template.getCode());
                }
            } catch (Exception e) {
                log.error("Error al crear plantilla {}: {}", template.getCode(), e.getMessage());
            }
        }
    }

    /**
     * Crea la lista de plantillas predeterminadas
     */
    private List<TemplateDto> createDefaultTemplates() {
        List<TemplateDto> templates = new ArrayList<>();

        try {
            // Plantilla de confirmación de pago
            templates.add(TemplateDto.builder()
                    .code("payment_confirmation")
                    .name("Confirmación de Pago")
                    .type(NotificationType.EMAIL)
                    .subject("Confirmación de Pago - ${paymentReference}")
                    .content(loadTemplateContent("payment-confirmation.html"))
                    .isActive(true)
                    .eventType("PAYMENT_SUCCESSFUL")
                    .description("Confirmación de pago recibido correctamente")
                    .build());

            // Plantilla de pago fallido
            templates.add(TemplateDto.builder()
                    .code("payment_failed")
                    .name("Pago Fallido")
                    .type(NotificationType.EMAIL)
                    .subject("Pago No Procesado - ${paymentReference}")
                    .content("Estimado/a ${customerName},\n\n" +
                            "Lamentamos informarle que el pago para su póliza ${policyNumber} no pudo ser procesado.\n\n" +
                            "Referencia: ${paymentReference}\n" +
                            "Monto: ${amount} ${currency}\n" +
                            "Fecha: ${paymentDate}\n\n" +
                            "Por favor, verifique su método de pago o contacte con nuestro servicio de atención al cliente.\n\n" +
                            "Atentamente,\n" +
                            "Equipo de InsureTech")
                    .isActive(true)
                    .eventType("PAYMENT_FAILED")
                    .description("Notificación de pago fallido")
                    .build());

            // Plantilla de recordatorio de renovación
            templates.add(TemplateDto.builder()
                    .code("policy_expiring_soon")
                    .name("Recordatorio de Renovación")
                    .type(NotificationType.EMAIL)
                    .subject("Su póliza ${policyNumber} vence pronto")
                    .content(loadTemplateContent("policy-renewal.html"))
                    .isActive(true)
                    .eventType("EXPIRING_SOON")
                    .description("Recordatorio de vencimiento próximo de póliza")
                    .build());

            // Plantilla de actualización de estado de reclamación
            templates.add(TemplateDto.builder()
                    .code("claim_status_update")
                    .name("Actualización Estado Reclamación")
                    .type(NotificationType.EMAIL)
                    .subject("Actualización de su reclamación ${claimNumber}")
                    .content(loadTemplateContent("claim-status-update.html"))
                    .isActive(true)
                    .eventType("CLAIM_STATUS_CHANGE")
                    .description("Notificación de cambio de estado en reclamación")
                    .build());

            // Plantilla SMS de pago confirmado
            templates.add(TemplateDto.builder()
                    .code("payment_confirmation_SMS")
                    .name("SMS Confirmación Pago")
                    .type(NotificationType.SMS)
                    .subject("Confirmación de Pago")
                    .content("InsureTech: Su pago de ${amount} ${currency} para la póliza ${policyNumber} " +
                            "ha sido confirmado. Ref: ${paymentReference}")
                    .isActive(true)
                    .eventType("PAYMENT_SUCCESSFUL")
                    .description("SMS de confirmación de pago")
                    .build());

            // Plantilla SMS de reclamación aprobada
            templates.add(TemplateDto.builder()
                    .code("claim_approved_SMS")
                    .name("SMS Reclamación Aprobada")
                    .type(NotificationType.SMS)
                    .subject("Reclamación Aprobada")
                    .content("InsureTech: Su reclamación ${claimNumber} ha sido APROBADA. " +
                            "Monto: ${approvedAmount}. Para más detalles, visite su portal de cliente.")
                    .isActive(true)
                    .eventType("CLAIM_APPROVED")
                    .description("SMS de notificación de reclamación aprobada")
                    .build());
        } catch (IOException e) {
            log.error("Error cargando plantillas predeterminadas: {}", e.getMessage());
        }

        return templates;
    }

    /**
     * Carga el contenido de un archivo de plantilla
     */
    private String loadTemplateContent(String templateName) throws IOException {
        Resource resource = new ClassPathResource("templates/email/" + templateName);

        try (Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
            return FileCopyUtils.copyToString(reader);
        }
    }
}
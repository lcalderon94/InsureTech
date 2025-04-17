package com.insurtech.notification.util;

import com.insurtech.notification.model.dto.TemplateDto;
import com.insurtech.notification.model.enums.NotificationType;
import com.insurtech.notification.service.interfaces.TemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class AutoTemplateLoader implements CommandLineRunner {
    private final TemplateService templateService;
    private final ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    @Override
    public void run(String... args) throws Exception {
        log.info("Iniciando carga automática de plantillas");
        loadEmailTemplates();
    }

    private void loadEmailTemplates() throws IOException {
        Resource[] resources = resolver.getResources("classpath:templates/email/*.html");
        log.info("Encontradas {} plantillas de email", resources.length);

        for (Resource resource : resources) {
            String filename = resource.getFilename();
            if (filename == null) continue;

            // Extraer código: payment-processed-email.html → payment-processed
            String templateCode = filename.replace("-email.html", "");

            // Verificar si ya existe
            if (templateService.findTemplateByCode(templateCode).isEmpty()) {
                // Cargar contenido del archivo HTML
                String content = new String(FileCopyUtils.copyToByteArray(resource.getInputStream()));

                // Crear título legible: Payment Processed
                String name = Arrays.stream(templateCode.split("-"))
                        .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
                        .collect(Collectors.joining(" "));

                // Evento correspondiente: payment.processed
                String eventType = templateCode.replace("-", ".");

                // Crear plantilla
                TemplateDto template = TemplateDto.builder()
                        .code(templateCode)
                        .name(name)
                        .type(NotificationType.EMAIL)
                        .subject(name)
                        .content(content)
                        .isActive(true)
                        .eventType(eventType.toUpperCase())
                        .description("Plantilla para evento " + eventType)
                        .build();

                templateService.createTemplate(template);
                log.info("Plantilla registrada automáticamente: {}", templateCode);
            } else {
                log.debug("Plantilla ya existente: {}", templateCode);
            }
        }
    }
}

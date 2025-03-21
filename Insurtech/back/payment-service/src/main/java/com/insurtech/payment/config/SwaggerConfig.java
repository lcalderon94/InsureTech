package com.insurtech.payment.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de Swagger/OpenAPI para documentación de la API
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "API de Gestión de Pagos - InsureTech",
                version = "1.0",
                description = "API para la gestión de pagos, reembolsos, facturación y métodos de pago en la plataforma InsureTech",
                contact = @Contact(
                        name = "Equipo InsureTech",
                        email = "soporte@insurtech.com"
                )
        )
)
@SecurityScheme(
        name = "bearer-jwt",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class SwaggerConfig {
}
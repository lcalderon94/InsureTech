package com.insurtech.notification.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.util.Map;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "API de Servicio de Notificaciones",
                version = "1.0",
                description = "API para gestionar el envío de notificaciones en la plataforma InsureTech",
                contact = @Contact(
                        name = "Equipo de Desarrollo InsureTech",
                        email = "developers@insurtech.com",
                        url = "https://developers.insurtech.com"
                ),
                license = @License(
                        name = "Privada",
                        url = "https://www.insurtech.com/terms"
                )
        ),
        servers = {
                @Server(url = "/", description = "Servidor por defecto"),
                @Server(url = "http://localhost:9500", description = "Servidor de desarrollo local")
        },
        security = {
                @SecurityRequirement(name = "bearer-key")
        }
)
@SecurityScheme(
        name = "bearer-key",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class OpenApiConfig {

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("public-api")
                .pathsToMatch("/api/notifications/**")
                .build();
    }

    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("admin-api")
                .pathsToMatch("/api/notifications/admin/**")
                .build();
    }

    @Bean
    public OpenAPI customOpenAPI() {
        Schema<LocalDateTime> localDateTimeSchema = new Schema<LocalDateTime>()
                .type("string")
                .format("date-time")
                .example("2025-03-30T12:30:00");

        return new OpenAPI()
                .components(new Components()
                        .schemas(Map.of("LocalDateTime", localDateTimeSchema))
                        .addSecuritySchemes("bearer-key",
                                new io.swagger.v3.oas.models.security.SecurityScheme()
                                        .type(io.swagger.v3.oas.models.security.SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT"))
                );
    }
}
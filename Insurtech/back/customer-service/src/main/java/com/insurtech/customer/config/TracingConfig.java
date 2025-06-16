package com.insurtech.customer.config;

import io.opentelemetry.exporter.zipkin.ZipkinSpanExporter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de trazas para enviar eventos a Zipkin/OpenTelemetry.
 */
@Configuration
public class TracingConfig {

    /**
     * Exportador de spans hacia Zipkin
     */
    @Bean
    public ZipkinSpanExporter zipkinSpanExporter(
            @Value("${tracing.zipkin.url:http://localhost:9411/api/v2/spans}") String url) {
        return ZipkinSpanExporter.builder()
                .setEndpoint(url)
                .build();
    }
}

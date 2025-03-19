package com.insurtech.quote.config;

import com.insurtech.quote.filter.AuthHeaderFilter;
import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignClientConfig {

    /**
     * Interceptor para propagar el token JWT a los microservicios llamados por Feign
     */
    @Bean
    public RequestInterceptor requestTokenBearerInterceptor() {
        return requestTemplate -> {
            // Recuperar el token del almacenamiento que fue guardado por el filtro
            String token = AuthHeaderFilter.getAuthHeader();
            if (token != null && !token.isEmpty()) {
                requestTemplate.header("Authorization", token);
            }
        };
    }

    /**
     * Decodificador de errores personalizado para Feign
     */
    @Bean
    public ErrorDecoder errorDecoder() {
        return (methodKey, response) -> {
            int status = response.status();

            if (status >= 400 && status < 500) {
                String errorMessage;
                try {
                    errorMessage = new String(response.body().asInputStream().readAllBytes());
                } catch (Exception e) {
                    errorMessage = "Error en la llamada a " + methodKey + ": " + response.reason();
                }

                return new RuntimeException("Error en servicio remoto: " + errorMessage);
            }

            return new Exception("Error en la llamada al servicio remoto. Status: " + status);
        };
    }
}
package com.insurtech.policy.config;

import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@Configuration
public class FeignClientConfig {

    /**
     * Interceptor para propagar el token JWT a los microservicios llamados por Feign
     */
    @Bean
    public RequestInterceptor requestTokenBearerInterceptor() {
        return requestTemplate -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication instanceof JwtAuthenticationToken) {
                JwtAuthenticationToken jwtAuthentication = (JwtAuthenticationToken) authentication;
                requestTemplate.header("Authorization", "Bearer " + jwtAuthentication.getToken().getTokenValue());
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
package com.insurtech.payment.config;

import com.insurtech.payment.exception.PaymentServiceErrorDecoder;
import feign.Logger;
import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Configuración para clientes Feign que se comunican con otros microservicios
 */
@Configuration
public class FeignClientConfig {

    /**
     * Configura el nivel de log para clientes Feign
     */
    @Bean
    Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }

    /**
     * Interceptor para propagar el token JWT a otros microservicios
     *
     * Esto asegura que las solicitudes autenticadas mantengan el token
     * al comunicarse con otros servicios.
     */
    @Bean
    public RequestInterceptor requestTokenBearerInterceptor() {
        return requestTemplate -> {
            ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (requestAttributes != null) {
                String authorizationHeader = requestAttributes.getRequest().getHeader("Authorization");
                if (authorizationHeader != null && !authorizationHeader.isEmpty()) {
                    requestTemplate.header("Authorization", authorizationHeader);
                }
            }
        };
    }

    /**
     * Decodificador de errores para clientes Feign
     *
     * Permite manejar errores HTTP de manera específica para este servicio
     */
    @Bean
    public PaymentServiceErrorDecoder paymentServiceErrorDecoder() {
        return new PaymentServiceErrorDecoder();
    }
}
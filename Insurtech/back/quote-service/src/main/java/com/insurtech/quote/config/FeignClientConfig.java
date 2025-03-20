package com.insurtech.quote.config;

import com.insurtech.quote.util.TokenContext;
import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignClientConfig {

    /**
     * Interceptor para propagar el token JWT a las llamadas Feign.
     */
    @Bean
    public RequestInterceptor requestTokenBearerInterceptor() {
        return requestTemplate -> {
            try {
                String token = TokenContext.getToken();
                if (token != null) {
                    requestTemplate.header("Authorization", token);
                }
            } catch (Exception e) {
                System.err.println("Error al obtener token de autenticación: " + e.getMessage());
            }
        };
    }

    /**
     * Decodificador de errores personalizado (puedes ajustarlo según tus necesidades).
     */
    @Bean
    public ErrorDecoder errorDecoder() {
        return new ErrorDecoder.Default();
    }
}

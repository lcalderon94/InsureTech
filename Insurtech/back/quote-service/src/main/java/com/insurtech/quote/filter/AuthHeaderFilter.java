package com.insurtech.quote.filter;

import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
public class AuthHeaderFilter implements WebFilter {

    private static final ThreadLocal<String> AUTH_HEADER = new ThreadLocal<>();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        // Extraer el encabezado de autorización de la solicitud entrante
        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");

        if (authHeader != null) {
            // Almacenar en ThreadLocal para que esté disponible durante esta solicitud
            AUTH_HEADER.set(authHeader);
        }

        return chain.filter(exchange)
                .doFinally(signalType -> {
                    // Limpiar el ThreadLocal después de completar la solicitud
                    AUTH_HEADER.remove();
                });
    }

    /**
     * Método estático para obtener el encabezado de autorización almacenado
     */
    public static String getAuthHeader() {
        return AUTH_HEADER.get();
    }
}
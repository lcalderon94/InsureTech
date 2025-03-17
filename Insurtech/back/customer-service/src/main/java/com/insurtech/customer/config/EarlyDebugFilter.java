package com.insurtech.customer.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class EarlyDebugFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        System.out.println("=== SOLICITUD ENTRANTE ===");
        System.out.println("URI: " + httpRequest.getRequestURI());
        System.out.println("Método: " + httpRequest.getMethod());
        System.out.println("Encabezado Authorization: " + httpRequest.getHeader("Authorization"));
        System.out.println("==========================");

        chain.doFilter(request, response);
    }
}
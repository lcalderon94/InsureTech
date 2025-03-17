package com.insurtech.customer.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Estructura para respuestas de error estandarizadas
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

    /**
     * Código de estado HTTP
     */
    private int status;

    /**
     * Mensaje de error
     */
    private String message;

    /**
     * Ruta donde ocurrió el error
     */
    private String path;

    /**
     * Marca de tiempo del error
     */
    private LocalDateTime timestamp;
}
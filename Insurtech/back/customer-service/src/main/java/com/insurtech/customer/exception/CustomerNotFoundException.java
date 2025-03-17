package com.insurtech.customer.exception;

/**
 * Excepción para cuando un cliente no es encontrado
 */
public class CustomerNotFoundException extends RuntimeException {

    public CustomerNotFoundException(String message) {
        super(message);
    }

    public CustomerNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}


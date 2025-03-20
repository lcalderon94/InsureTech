package com.insurtech.claim.exception;

public class ClaimNotFoundException extends RuntimeException {

    public ClaimNotFoundException(String message) {
        super(message);
    }

    public ClaimNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
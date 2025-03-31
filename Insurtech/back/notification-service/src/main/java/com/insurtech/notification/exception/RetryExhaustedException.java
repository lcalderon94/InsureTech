package com.insurtech.notification.exception;

public class RetryExhaustedException extends NotificationException {

    public RetryExhaustedException(String message) {
        super(message);
    }

    public RetryExhaustedException(String message, Throwable cause) {
        super(message, cause);
    }
}
package com.insurtech.notification.exception;

public class TemplateNotFoundException extends NotificationException {

    public TemplateNotFoundException(String message) {
        super(message);
    }

    public TemplateNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
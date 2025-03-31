package com.insurtech.notification.exception;

public class InvalidTemplateException extends NotificationException {

    public InvalidTemplateException(String message) {
        super(message);
    }

    public InvalidTemplateException(String message, Throwable cause) {
        super(message, cause);
    }
}
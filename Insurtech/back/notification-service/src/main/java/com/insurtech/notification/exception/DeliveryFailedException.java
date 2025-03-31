package com.insurtech.notification.exception;

public class DeliveryFailedException extends NotificationException {

    public DeliveryFailedException(String message) {
        super(message);
    }

    public DeliveryFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
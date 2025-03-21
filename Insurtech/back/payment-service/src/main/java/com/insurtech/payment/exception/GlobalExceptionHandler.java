package com.insurtech.payment.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException exception, WebRequest request) {

        ErrorResponse errorResponse = createErrorResponse(
                HttpStatus.NOT_FOUND,
                exception.getMessage(),
                request,
                exception
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(PaymentProcessingException.class)
    public ResponseEntity<ErrorResponse> handlePaymentProcessingException(
            PaymentProcessingException exception, WebRequest request) {

        ErrorResponse errorResponse = createErrorResponse(
                HttpStatus.UNPROCESSABLE_ENTITY,
                exception.getMessage(),
                request,
                exception
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @ExceptionHandler(TransactionFailedException.class)
    public ResponseEntity<ErrorResponse> handleTransactionFailedException(
            TransactionFailedException exception, WebRequest request) {

        ErrorResponse errorResponse = createErrorResponse(
                HttpStatus.SERVICE_UNAVAILABLE,
                exception.getMessage(),
                request,
                exception
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.SERVICE_UNAVAILABLE);
    }

    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientFundsException(
            InsufficientFundsException exception, WebRequest request) {

        ErrorResponse errorResponse = createErrorResponse(
                HttpStatus.PAYMENT_REQUIRED,
                exception.getMessage(),
                request,
                exception
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.PAYMENT_REQUIRED);
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ErrorResponse> handleSecurityException(
            SecurityException exception, WebRequest request) {

        ErrorResponse errorResponse = createErrorResponse(
                HttpStatus.FORBIDDEN,
                exception.getMessage(),
                request,
                exception
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(
            MethodArgumentNotValidException exception) {

        Map<String, Object> response = new HashMap<>();
        Map<String, String> errors = new HashMap<>();

        exception.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        response.put("status", "BAD_REQUEST");
        response.put("statusCode", HttpStatus.BAD_REQUEST.value());
        response.put("message", "Error de validación");
        response.put("errors", errors);
        response.put("timestamp", LocalDateTime.now());

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(
            Exception exception, WebRequest request) {

        ErrorResponse errorResponse = createErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Se ha producido un error inesperado",
                request,
                exception
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ErrorResponse createErrorResponse(
            HttpStatus status, String message, WebRequest request, Exception exception) {

        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setStatus(status.name());
        errorResponse.setStatusCode(status.value());
        errorResponse.setMessage(message);
        errorResponse.setTimestamp(LocalDateTime.now());

        if (request instanceof ServletWebRequest) {
            errorResponse.setPath(((ServletWebRequest) request).getRequest().getRequestURI());
        }

        errorResponse.setException(exception.getClass().getSimpleName());

        return errorResponse;
    }
}
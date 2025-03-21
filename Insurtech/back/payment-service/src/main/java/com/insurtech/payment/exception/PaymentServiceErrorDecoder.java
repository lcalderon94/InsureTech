package com.insurtech.payment.exception;

import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Slf4j
public class PaymentServiceErrorDecoder implements ErrorDecoder {

    private final ErrorDecoder defaultErrorDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        HttpStatus responseStatus = HttpStatus.valueOf(response.status());

        if (responseStatus.is4xxClientError()) {
            String errorMessage = extractErrorMessage(response);
            log.error("Error de cliente en llamada Feign. Método: {}, Status: {}, Error: {}",
                    methodKey, responseStatus, errorMessage);

            switch (responseStatus) {
                case NOT_FOUND:
                    return new ResourceNotFoundException(errorMessage);
                case BAD_REQUEST:
                    return new PaymentProcessingException(errorMessage);
                case UNAUTHORIZED:
                case FORBIDDEN:
                    return new SecurityException(errorMessage);
                default:
                    return new RuntimeException("Error de cliente: " + errorMessage);
            }
        } else if (responseStatus.is5xxServerError()) {
            String errorMessage = extractErrorMessage(response);
            log.error("Error de servidor en llamada Feign. Método: {}, Status: {}, Error: {}",
                    methodKey, responseStatus, errorMessage);
            return new TransactionFailedException("Error de servidor externo: " + errorMessage);
        }

        return defaultErrorDecoder.decode(methodKey, response);
    }

    private String extractErrorMessage(Response response) {
        try (InputStream inputStream = response.body().asInputStream()) {
            byte[] bytes = inputStream.readAllBytes();
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Error al extraer mensaje de error de la respuesta", e);
            return "No se pudo extraer el mensaje de error";
        }
    }
}
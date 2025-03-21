package com.insurtech.payment.client.fallback;

import com.insurtech.payment.client.CustomerServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;

@Slf4j
@Component
public class CustomerServiceFallback implements CustomerServiceClient {

    @Override
    public ResponseEntity<Map<String, Object>> getCustomerByNumber(String customerNumber) {
        log.error("Fallback: No se pudo obtener el cliente con número: {}", customerNumber);
        return ResponseEntity.ok(Collections.singletonMap("error", "Servicio de clientes no disponible"));
    }

    @Override
    public ResponseEntity<Map<String, Object>> getCustomerByEmail(String email) {
        log.error("Fallback: No se pudo obtener el cliente con email: {}", email);
        return ResponseEntity.ok(Collections.singletonMap("error", "Servicio de clientes no disponible"));
    }

    @Override
    public ResponseEntity<Map<String, Object>> getCustomerByIdentification(String identificationNumber, String identificationType) {
        log.error("Fallback: No se pudo obtener el cliente con identificación: {}/{}", identificationNumber, identificationType);
        return ResponseEntity.ok(Collections.singletonMap("error", "Servicio de clientes no disponible"));
    }

    @Override
    public ResponseEntity<Void> sendNotification(Map<String, Object> notification) {
        log.error("Fallback: No se pudo enviar la notificación al cliente");
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Boolean> customerExists(String customerNumber) {
        log.error("Fallback: No se pudo verificar la existencia del cliente: {}", customerNumber);
        return ResponseEntity.ok(false);
    }
}
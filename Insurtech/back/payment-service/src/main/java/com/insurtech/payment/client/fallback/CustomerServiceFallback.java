package com.insurtech.payment.client.fallback;

import com.insurtech.payment.client.CustomerServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;

@Slf4j
@Component
public class CustomerServiceFallback implements CustomerServiceClient {

    @Override
    public Map<String, Object> getCustomerByNumber(String customerNumber) {
        log.error("Fallback: No se pudo obtener el cliente con número: {}", customerNumber);
        return Collections.singletonMap("error", "Servicio de clientes no disponible");
    }

    @Override
    public Map<String, Object> getCustomerByEmail(String email) {
        log.error("Fallback: No se pudo obtener el cliente con email: {}", email);
        return Collections.singletonMap("error", "Servicio de clientes no disponible");
    }

    @Override
    public Map<String, Object> getCustomerByIdentification(String identificationNumber, String identificationType) {
        log.error("Fallback: No se pudo obtener el cliente con identificación: {}/{}", identificationNumber, identificationType);
        return Collections.singletonMap("error", "Servicio de clientes no disponible");
    }

    @Override
    public void sendNotification(String jobId, Map<String, Object> notification) {
        log.error("Fallback: No se pudo enviar la notificación al cliente");
    }
}
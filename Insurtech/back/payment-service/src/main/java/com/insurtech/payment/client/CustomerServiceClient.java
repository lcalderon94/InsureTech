package com.insurtech.payment.client;

import com.insurtech.payment.client.fallback.CustomerServiceFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@FeignClient(name = "customer-service", fallback = CustomerServiceFallback.class)
public interface CustomerServiceClient {

    @GetMapping("/api/customers/number/{customerNumber}")
    ResponseEntity<Map<String, Object>> getCustomerByNumber(@PathVariable String customerNumber);

    @GetMapping("/api/customers/email/{email}")
    ResponseEntity<Map<String, Object>> getCustomerByEmail(@PathVariable String email);

    @GetMapping("/api/customers/identification")
    ResponseEntity<Map<String, Object>> getCustomerByIdentification(
            @RequestParam String identificationNumber,
            @RequestParam String identificationType);

    /**
     * Adaptado para usar el endpoint /api/customers/batch/status/{jobId}
     * como proxy para enviar notificaciones
     */
    @PostMapping("/api/customers/batch/status/{jobId}")
    ResponseEntity<Void> sendNotification(@PathVariable("jobId") String jobId, @RequestBody Map<String, Object> notification);

    /**
     * Verificar si existe un cliente
     * Intentamos obtener el cliente y verificamos si existe
     */
    @GetMapping("/api/customers/number/{customerNumber}")
    ResponseEntity<Boolean> customerExists(@PathVariable String customerNumber);
}
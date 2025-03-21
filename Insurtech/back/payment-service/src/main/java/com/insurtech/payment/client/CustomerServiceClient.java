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

    @PostMapping("/api/customers/notification")
    ResponseEntity<Void> sendNotification(@RequestBody Map<String, Object> notification);

    @GetMapping("/api/customers/number/{customerNumber}/exists")
    ResponseEntity<Boolean> customerExists(@PathVariable String customerNumber);
}
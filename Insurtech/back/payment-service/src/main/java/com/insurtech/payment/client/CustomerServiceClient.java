package com.insurtech.payment.client;

import com.insurtech.payment.client.fallback.CustomerServiceFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@FeignClient(name = "customer-service", fallback = CustomerServiceFallback.class)
public interface CustomerServiceClient {

    @GetMapping("/api/customers/number/{customerNumber}")
    Map<String, Object> getCustomerByNumber(@PathVariable String customerNumber);

    @GetMapping("/api/customers/email/{email}")
    Map<String, Object> getCustomerByEmail(@PathVariable String email);

    @GetMapping("/api/customers/identification")
    Map<String, Object> getCustomerByIdentification(
            @RequestParam String identificationNumber,
            @RequestParam String identificationType);

    @PostMapping("/api/customers/batch/status/{jobId}")
    void sendNotification(@PathVariable("jobId") String jobId, @RequestBody Map<String, Object> notification);
}
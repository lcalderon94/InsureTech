package com.insurtech.claim.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(name = "customer-service", url = "${services.customer-service.url}")
public interface CustomerClient {

    @GetMapping("/api/customers/{id}")
    Map<String, Object> getCustomerById(@PathVariable Long id);

    @GetMapping("/api/customers/email/{email}")
    Map<String, Object> getCustomerByEmail(@PathVariable String email);

    @GetMapping("/api/customers/number/{customerNumber}")
    Map<String, Object> getCustomerByNumber(@PathVariable String customerNumber);

    @GetMapping("/api/customers/identification")
    Map<String, Object> getCustomerByIdentification(
            @RequestParam String identificationNumber,
            @RequestParam String identificationType);
}
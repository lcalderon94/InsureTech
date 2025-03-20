package com.insurtech.quote.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;
import java.util.Map;

@FeignClient(name = "policy-service", url = "${services.policy-service.url}")
public interface PolicyClient {
    // Solo métodos basados en identificadores de negocio
    @GetMapping("/api/policies/number/{policyNumber}")
    Map<String, Object> getPolicyByNumber(@PathVariable String policyNumber);

    @GetMapping("/api/policies/search/by-customer-email")
    List<Map<String, Object>> getPoliciesByCustomerEmail(@RequestParam String email);

    @GetMapping("/api/policies/search/by-customer-identification")
    List<Map<String, Object>> getPoliciesByCustomerIdentification(
            @RequestParam String identificationNumber,
            @RequestParam String identificationType);

    @GetMapping("/api/policies/search/by-customer-number")
    List<Map<String, Object>> getPoliciesByCustomerNumber(@RequestParam String customerNumber);
}
package com.insurtech.claim.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;
import java.util.List;

@FeignClient(name = "policy-service", url = "${services.policy-service.url}")
public interface PolicyClient {

    @GetMapping("/api/policies/{id}")
    Map<String, Object> getPolicyById(@PathVariable Long id);

    @GetMapping("/api/policies/number/{policyNumber}")
    Map<String, Object> getPolicyByNumber(@PathVariable String policyNumber);

    @GetMapping("/api/policies/customer/{customerId}")
    List<Map<String, Object>> getPoliciesByCustomerId(@PathVariable Long customerId);

    @GetMapping("/api/policies/customer/{customerNumber}")
    List<Map<String, Object>> getPoliciesByCustomerNumber(@PathVariable String customerNumber);
}
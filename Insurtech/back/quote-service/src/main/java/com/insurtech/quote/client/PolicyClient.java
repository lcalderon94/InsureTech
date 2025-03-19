package com.insurtech.quote.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient(name = "policy-service", url = "${services.policy-service.url}")
public interface PolicyClient {

    @GetMapping("/api/policies/customer/{customerId}")
    List<Map<String, Object>> getPoliciesByCustomerId(@PathVariable Long customerId);

    @GetMapping("/api/policies/{id}")
    Map<String, Object> getPolicyById(@PathVariable Long id);

    @GetMapping("/api/policies/number/{policyNumber}")
    Map<String, Object> getPolicyByNumber(@PathVariable String policyNumber);
}
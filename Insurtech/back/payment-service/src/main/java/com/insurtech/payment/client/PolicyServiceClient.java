package com.insurtech.payment.client;

import com.insurtech.payment.client.fallback.PolicyServiceFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.List;

@FeignClient(name = "policy-service", fallback = PolicyServiceFallback.class)
public interface PolicyServiceClient {

    @GetMapping("/api/policies/number/{policyNumber}")
    Map<String, Object> getPolicyByNumber(@PathVariable String policyNumber);

    @GetMapping("/api/policies/customer/{customerId}")
    List<Map<String, Object>> getPoliciesByCustomerNumber(@PathVariable("customerId") String customerNumber);

    @PatchMapping("/api/policies/number/{policyNumber}/status")
    Map<String, Object> updatePolicyStatus(
            @PathVariable String policyNumber,
            @RequestParam String status,
            @RequestParam String reason);
}
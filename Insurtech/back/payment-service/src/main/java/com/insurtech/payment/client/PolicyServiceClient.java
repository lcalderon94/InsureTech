package com.insurtech.payment.client;

import com.insurtech.payment.client.fallback.PolicyServiceFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(name = "policy-service", fallback = PolicyServiceFallback.class)
public interface PolicyServiceClient {

    @GetMapping("/api/policies/number/{policyNumber}")
    ResponseEntity<Map<String, Object>> getPolicyByNumber(@PathVariable String policyNumber);

    @GetMapping("/api/policies/customer/{customerNumber}")
    ResponseEntity<Object> getPoliciesByCustomerNumber(@PathVariable String customerNumber);

    @PatchMapping("/api/policies/number/{policyNumber}/status")
    ResponseEntity<Map<String, Object>> updatePolicyStatus(
            @PathVariable String policyNumber,
            @RequestParam String status,
            @RequestParam String reason);

    @GetMapping("/api/policies/number/{policyNumber}/exists")
    ResponseEntity<Boolean> policyExists(@PathVariable String policyNumber);
}
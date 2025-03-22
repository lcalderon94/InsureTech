package com.insurtech.payment.client;

import com.insurtech.payment.client.fallback.PolicyServiceFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@FeignClient(name = "policy-service", fallback = PolicyServiceFallback.class)
public interface PolicyServiceClient {

    @GetMapping("/api/policies/number/{policyNumber}")
    ResponseEntity<Map<String, Object>> getPolicyByNumber(@PathVariable String policyNumber);

    /**
     * Adaptado para usar el endpoint correcto con customerId en lugar de customerNumber
     */
    @GetMapping("/api/policies/customer/{customerId}")
    ResponseEntity<Object> getPoliciesByCustomerNumber(@PathVariable("customerId") String customerNumber);

    @PatchMapping("/api/policies/number/{policyNumber}/status")
    ResponseEntity<Map<String, Object>> updatePolicyStatus(
            @PathVariable String policyNumber,
            @RequestParam String status,
            @RequestParam String reason);

    /**
     * Verificar si existe una póliza
     * Intentamos obtener la póliza y verificamos si existe
     */
    @GetMapping("/api/policies/number/{policyNumber}")
    ResponseEntity<Boolean> policyExists(@PathVariable String policyNumber);
}
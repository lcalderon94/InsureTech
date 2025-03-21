package com.insurtech.payment.client;

import com.insurtech.payment.client.fallback.ClaimServiceFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.Map;

@FeignClient(name = "claim-service", fallback = ClaimServiceFallback.class)
public interface ClaimServiceClient {

    @GetMapping("/api/claims/number/{claimNumber}")
    ResponseEntity<Map<String, Object>> getClaimByNumber(@PathVariable String claimNumber);

    @GetMapping("/api/claims/policy/{policyNumber}")
    ResponseEntity<Object> getClaimsByPolicyNumber(@PathVariable String policyNumber);

    @GetMapping("/api/claims/customer/{customerNumber}")
    ResponseEntity<Object> getClaimsByCustomerNumber(@PathVariable String customerNumber);

    @PatchMapping("/api/claims/number/{claimNumber}/status")
    ResponseEntity<Map<String, Object>> updateClaimStatus(
            @PathVariable String claimNumber,
            @RequestBody Map<String, Object> statusUpdateDto);

    @GetMapping("/api/claims/number/{claimNumber}/exists")
    ResponseEntity<Boolean> claimExists(@PathVariable String claimNumber);
}
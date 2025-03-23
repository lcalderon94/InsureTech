package com.insurtech.payment.client;

import com.insurtech.payment.client.fallback.ClaimServiceFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.List;

@FeignClient(name = "claim-service", fallback = ClaimServiceFallback.class)
public interface ClaimServiceClient {

    @GetMapping("/api/claims/number/{claimNumber}")
    Map<String, Object> getClaimByNumber(@PathVariable String claimNumber);

    @GetMapping("/api/claims/policy/{policyNumber}")
    List<Map<String, Object>> getClaimsByPolicyNumber(@PathVariable String policyNumber);

    @GetMapping("/api/claims/customer/{customerNumber}")
    List<Map<String, Object>> getClaimsByCustomerNumber(@PathVariable String customerNumber);

    @PatchMapping("/api/claims/number/{claimNumber}/status")
    Map<String, Object> updateClaimStatus(
            @PathVariable String claimNumber,
            @RequestBody Map<String, Object> statusUpdateDto);
}
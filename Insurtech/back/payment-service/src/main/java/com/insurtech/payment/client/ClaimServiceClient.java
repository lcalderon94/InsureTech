package com.insurtech.payment.client;

import com.insurtech.payment.client.fallback.ClaimServiceFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@FeignClient(name = "claim-service", fallback = ClaimServiceFallback.class)
public interface ClaimServiceClient {

    @GetMapping("/api/claims/number/{claimNumber}")
    ResponseEntity<Map<String, Object>> getClaimByNumber(@PathVariable String claimNumber);

    @GetMapping("/api/claims/policy/{policyNumber}")
    ResponseEntity<Object> getClaimsByPolicyNumber(@PathVariable String policyNumber);

    @GetMapping("/api/claims/customer/{customerNumber}")
    ResponseEntity<Object> getClaimsByCustomerNumber(@PathVariable String customerNumber);

    /**
     * Actualiza el estado de una reclamación
     * Note: En el servicio original espera un ClaimStatusUpdateDto, pero aquí usamos Map
     * para evitar dependencias entre microservicios
     */
    @PatchMapping("/api/claims/number/{claimNumber}/status")
    ResponseEntity<Map<String, Object>> updateClaimStatus(
            @PathVariable String claimNumber,
            @RequestBody Map<String, Object> statusUpdateDto);

    /**
     * Verifica si existe una reclamación
     * Implementación adaptada: intentamos obtener el claim y verificamos si existe
     */
    @GetMapping("/api/claims/number/{claimNumber}")
    ResponseEntity<Boolean> claimExists(@PathVariable String claimNumber);
}
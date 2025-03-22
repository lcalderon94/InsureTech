package com.insurtech.payment.client.fallback;

import com.insurtech.payment.client.ClaimServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;

@Slf4j
@Component
public class ClaimServiceFallback implements ClaimServiceClient {

    @Override
    public ResponseEntity<Map<String, Object>> getClaimByNumber(String claimNumber) {
        log.error("Fallback: No se pudo obtener la reclamación con número: {}", claimNumber);
        return ResponseEntity.ok(Collections.singletonMap("error", "Servicio de reclamaciones no disponible"));
    }

    @Override
    public ResponseEntity<Object> getClaimsByPolicyNumber(String policyNumber) {
        log.error("Fallback: No se pudieron obtener las reclamaciones de la póliza: {}", policyNumber);
        return ResponseEntity.ok(Collections.emptyList());
    }

    @Override
    public ResponseEntity<Object> getClaimsByCustomerNumber(String customerNumber) {
        log.error("Fallback: No se pudieron obtener las reclamaciones del cliente: {}", customerNumber);
        return ResponseEntity.ok(Collections.emptyList());
    }

    @Override
    public ResponseEntity<Map<String, Object>> updateClaimStatus(String claimNumber, Map<String, Object> statusUpdateDto) {
        log.error("Fallback: No se pudo actualizar el estado de la reclamación: {}", claimNumber);
        return ResponseEntity.ok(Collections.singletonMap("error", "Servicio de reclamaciones no disponible"));
    }

    @Override
    public ResponseEntity<Boolean> claimExists(String claimNumber) {
        log.error("Fallback: No se pudo verificar la existencia de la reclamación: {}", claimNumber);
        return ResponseEntity.ok(false);
    }
}
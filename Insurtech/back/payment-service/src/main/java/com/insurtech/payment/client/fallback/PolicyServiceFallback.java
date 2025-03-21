package com.insurtech.payment.client.fallback;

import com.insurtech.payment.client.PolicyServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;

@Slf4j
@Component
public class PolicyServiceFallback implements PolicyServiceClient {

    @Override
    public ResponseEntity<Map<String, Object>> getPolicyByNumber(String policyNumber) {
        log.error("Fallback: No se pudo obtener la póliza con número: {}", policyNumber);
        return ResponseEntity.ok(Collections.singletonMap("error", "Servicio de pólizas no disponible"));
    }

    @Override
    public ResponseEntity<Object> getPoliciesByCustomerNumber(String customerNumber) {
        log.error("Fallback: No se pudieron obtener las pólizas del cliente: {}", customerNumber);
        return ResponseEntity.ok(Collections.emptyList());
    }

    @Override
    public ResponseEntity<Map<String, Object>> updatePolicyStatus(String policyNumber, String status, String reason) {
        log.error("Fallback: No se pudo actualizar el estado de la póliza: {}", policyNumber);
        return ResponseEntity.ok(Collections.singletonMap("error", "Servicio de pólizas no disponible"));
    }

    @Override
    public ResponseEntity<Boolean> policyExists(String policyNumber) {
        log.error("Fallback: No se pudo verificar la existencia de la póliza: {}", policyNumber);
        return ResponseEntity.ok(false);
    }
}
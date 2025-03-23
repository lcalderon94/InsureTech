package com.insurtech.payment.client.fallback;

import com.insurtech.payment.client.PolicyServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;
import java.util.List;

@Slf4j
@Component
public class PolicyServiceFallback implements PolicyServiceClient {

    @Override
    public Map<String, Object> getPolicyByNumber(String policyNumber) {
        log.error("Fallback: No se pudo obtener la póliza con número: {}", policyNumber);
        return Collections.singletonMap("error", "Servicio de pólizas no disponible");
    }

    @Override
    public List<Map<String, Object>> getPoliciesByCustomerNumber(String customerNumber) {
        log.error("Fallback: No se pudieron obtener las pólizas del cliente: {}", customerNumber);
        return Collections.emptyList();
    }

    @Override
    public Map<String, Object> updatePolicyStatus(String policyNumber, String status, String reason) {
        log.error("Fallback: No se pudo actualizar el estado de la póliza: {}", policyNumber);
        return Collections.singletonMap("error", "Servicio de pólizas no disponible");
    }
}
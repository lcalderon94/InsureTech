package com.insurtech.payment.client.fallback;

import com.insurtech.payment.client.ClaimServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;
import java.util.List;

@Slf4j
@Component
public class ClaimServiceFallback implements ClaimServiceClient {

    @Override
    public Map<String, Object> getClaimByNumber(String claimNumber) {
        log.error("Fallback: No se pudo obtener la reclamación con número: {}", claimNumber);
        return Collections.singletonMap("error", "Servicio de reclamaciones no disponible");
    }

    @Override
    public List<Map<String, Object>> getClaimsByPolicyNumber(String policyNumber) {
        log.error("Fallback: No se pudieron obtener las reclamaciones de la póliza: {}", policyNumber);
        return Collections.emptyList();
    }

    @Override
    public List<Map<String, Object>> getClaimsByCustomerNumber(String customerNumber) {
        log.error("Fallback: No se pudieron obtener las reclamaciones del cliente: {}", customerNumber);
        return Collections.emptyList();
    }

    @Override
    public Map<String, Object> updateClaimStatus(String claimNumber, Map<String, Object> statusUpdateDto) {
        log.error("Fallback: No se pudo actualizar el estado de la reclamación: {}", claimNumber);
        return Collections.singletonMap("error", "Servicio de reclamaciones no disponible");
    }
}
package com.insurtech.policy.service.async;

import com.insurtech.policy.model.dto.PolicyDto;
import com.insurtech.policy.model.entity.Policy;
import com.insurtech.policy.repository.PolicyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class AsyncPolicyProcessor {

    private static final Logger log = LoggerFactory.getLogger(AsyncPolicyProcessor.class);

    private final PolicyRepository policyRepository;

    @Autowired
    public AsyncPolicyProcessor(PolicyRepository policyRepository) {
        this.policyRepository = policyRepository;
    }

    @Async("taskExecutor")
    public CompletableFuture<Double> calculatePremium(PolicyDto policyDto) {
        log.info("Calculando prima de forma asíncrona para póliza tipo: {}", policyDto.getPolicyType());

        try {
            // Simulación de cálculo complejo que toma tiempo
            Thread.sleep(2000);

            // Base de cálculo dependiendo del tipo de póliza
            double baseRate = switch (policyDto.getPolicyType()) {
                case AUTO -> 0.05;
                case HOME -> 0.03;
                case LIFE -> 0.07;
                case HEALTH -> 0.08;
                case TRAVEL -> 0.04;
                case BUSINESS -> 0.06;
                default -> 0.05;
            };

            // Cálculos basados en la suma asegurada
            double sumInsured = policyDto.getSumInsured().doubleValue();
            double premiumAmount = sumInsured * baseRate;

            // Ajustes por coberturas
            if (policyDto.getCoverages() != null && !policyDto.getCoverages().isEmpty()) {
                premiumAmount *= (1 + 0.1 * policyDto.getCoverages().size());
            }

            // Formateo del resultado final
            BigDecimal premium = new BigDecimal(premiumAmount).setScale(2, RoundingMode.HALF_UP);

            log.info("Cálculo de prima completado. Resultado: {}", premium);
            return CompletableFuture.completedFuture(premium.doubleValue());
        } catch (Exception e) {
            log.error("Error en cálculo asíncrono de prima", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    @Async("taskExecutor")
    public CompletableFuture<Map<String, Object>> calculateCustomerPolicyStatistics(Long customerId) {
        log.info("Calculando estadísticas de pólizas para cliente ID: {}", customerId);

        try {
            // Simulación de procesamiento que toma tiempo
            Thread.sleep(1500);

            Map<String, Object> statistics = new HashMap<>();
            List<Policy> customerPolicies = policyRepository.findByCustomerId(customerId);

            // Estadísticas básicas
            statistics.put("totalPolicies", customerPolicies.size());
            statistics.put("activePolicies", customerPolicies.stream()
                    .filter(p -> p.getStatus() == Policy.PolicyStatus.ACTIVE)
                    .count());

            // Distribución por tipo de póliza
            Map<Policy.PolicyType, Long> policyTypeDistribution = customerPolicies.stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                            Policy::getPolicyType,
                            java.util.stream.Collectors.counting()));
            statistics.put("policyTypeDistribution", policyTypeDistribution);

            // Suma total asegurada
            BigDecimal totalSumInsured = customerPolicies.stream()
                    .map(Policy::getSumInsured)
                    .filter(java.util.Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            statistics.put("totalSumInsured", totalSumInsured);

            // Prima total
            BigDecimal totalPremium = customerPolicies.stream()
                    .map(Policy::getPremium)
                    .filter(java.util.Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            statistics.put("totalPremium", totalPremium);

            log.info("Cálculo de estadísticas completado para cliente ID: {}", customerId);
            return CompletableFuture.completedFuture(statistics);
        } catch (Exception e) {
            log.error("Error en cálculo asíncrono de estadísticas", e);
            return CompletableFuture.failedFuture(e);
        }
    }
}
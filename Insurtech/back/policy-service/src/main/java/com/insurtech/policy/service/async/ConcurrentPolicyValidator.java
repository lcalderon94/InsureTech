package com.insurtech.policy.service.async;

import com.insurtech.policy.client.CustomerClient;
import com.insurtech.policy.model.dto.PolicyDto;
import com.insurtech.policy.model.dto.PolicyCoverageDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class ConcurrentPolicyValidator {

    private static final Logger log = LoggerFactory.getLogger(ConcurrentPolicyValidator.class);

    private final CustomerClient customerClient;

    @Autowired
    public ConcurrentPolicyValidator(CustomerClient customerClient) {
        this.customerClient = customerClient;
    }

    @Async("taskExecutor")
    public CompletableFuture<Boolean> validatePolicy(PolicyDto policyDto) {
        log.info("Iniciando validación concurrente de póliza para cliente ID: {}", policyDto.getCustomerId());

        try {
            List<String> validationErrors = new ArrayList<>();

            // Validación del cliente
            CompletableFuture<Boolean> customerValidation = validateCustomerAsync(policyDto.getCustomerId());

            // Validación de fechas
            validateDates(policyDto, validationErrors);

            // Validación de importes
            validateAmounts(policyDto, validationErrors);

            // Validación de coberturas
            validateCoverages(policyDto, validationErrors);

            // Esperar a que termine la validación del cliente
            boolean customerValid = customerValidation.get();
            if (!customerValid) {
                validationErrors.add("Cliente no encontrado o no válido");
            }

            // Resultado final
            boolean isValid = validationErrors.isEmpty();

            if (!isValid) {
                log.warn("Validación fallida para póliza. Errores: {}", validationErrors);
            } else {
                log.info("Validación exitosa para póliza del cliente ID: {}", policyDto.getCustomerId());
            }

            return CompletableFuture.completedFuture(isValid);
        } catch (Exception e) {
            log.error("Error en validación concurrente de póliza", e);
            return CompletableFuture.completedFuture(false);
        }
    }

    @Async("taskExecutor")
    private CompletableFuture<Boolean> validateCustomerAsync(Long customerId) {
        try {
            // Verificar que el cliente existe usando Feign Client
            customerClient.getCustomerById(customerId);
            return CompletableFuture.completedFuture(true);
        } catch (Exception e) {
            log.error("Error validando cliente ID: {}", customerId, e);
            return CompletableFuture.completedFuture(false);
        }
    }

    private void validateDates(PolicyDto policyDto, List<String> validationErrors) {
        // Fecha de inicio no puede ser nula
        if (policyDto.getStartDate() == null) {
            validationErrors.add("La fecha de inicio no puede ser nula");
        }

        // Fecha de fin no puede ser nula
        if (policyDto.getEndDate() == null) {
            validationErrors.add("La fecha de fin no puede ser nula");
        }

        // Fecha de fin debe ser posterior a fecha de inicio
        if (policyDto.getStartDate() != null && policyDto.getEndDate() != null
                && !policyDto.getEndDate().isAfter(policyDto.getStartDate())) {
            validationErrors.add("La fecha de fin debe ser posterior a la fecha de inicio");
        }

        // Fecha de inicio no puede ser anterior a hoy para nuevas pólizas
        if (policyDto.getId() == null && policyDto.getStartDate() != null
                && policyDto.getStartDate().isBefore(LocalDate.now())) {
            validationErrors.add("La fecha de inicio no puede ser anterior a hoy para nuevas pólizas");
        }
    }

    private void validateAmounts(PolicyDto policyDto, List<String> validationErrors) {
        // Prima debe ser mayor que cero
        if (policyDto.getPremium() != null && policyDto.getPremium().compareTo(BigDecimal.ZERO) <= 0) {
            validationErrors.add("La prima debe ser mayor que cero");
        }

        // Suma asegurada debe ser mayor que cero
        if (policyDto.getSumInsured() != null && policyDto.getSumInsured().compareTo(BigDecimal.ZERO) <= 0) {
            validationErrors.add("La suma asegurada debe ser mayor que cero");
        }
    }

    private void validateCoverages(PolicyDto policyDto, List<String> validationErrors) {
        // Debe tener al menos una cobertura
        if (policyDto.getCoverages() == null || policyDto.getCoverages().isEmpty()) {
            validationErrors.add("La póliza debe tener al menos una cobertura");
            return;
        }

        // Las coberturas deben tener una suma asegurada válida
        for (PolicyCoverageDto coverage : policyDto.getCoverages()) {
            if (coverage.getSumInsured() == null || coverage.getSumInsured().compareTo(BigDecimal.ZERO) <= 0) {
                validationErrors.add("La cobertura debe tener una suma asegurada válida");
                break;
            }
        }
    }
}
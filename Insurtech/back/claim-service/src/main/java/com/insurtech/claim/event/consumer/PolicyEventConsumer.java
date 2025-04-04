package com.insurtech.claim.event.consumer;

import com.insurtech.claim.model.entity.Claim;
import com.insurtech.claim.repository.ClaimRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true", matchIfMissing = false)
public class PolicyEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(PolicyEventConsumer.class);

    @Autowired
    private ClaimRepository claimRepository;

    @KafkaListener(topics = "policy.cancelled", groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory")
    public void consumePolicyCancelledEvent(Map<String, Object> event) {
        log.info("Recibido evento de póliza cancelada: {}", event);

        try {
            String policyNumber = (String) event.get("policyNumber");
            if (policyNumber == null) {
                log.error("Evento de póliza cancelada sin número de póliza");
                return;
            }

            // Obtener reclamaciones abiertas asociadas a esta póliza
            List<Claim> openClaims = claimRepository.findByPolicyNumber(policyNumber).stream()
                    .filter(claim -> isOpenClaim(claim.getStatus()))
                    .toList();

            if (openClaims.isEmpty()) {
                log.info("No hay reclamaciones abiertas para la póliza cancelada: {}", policyNumber);
                return;
            }

            log.info("Actualizando {} reclamaciones abiertas de la póliza cancelada: {}",
                    openClaims.size(), policyNumber);

            // Actualizar cada reclamación
            for (Claim claim : openClaims) {
                // Añadir nota sobre cancelación de póliza
                String comments = claim.getHandlerComments() != null ? claim.getHandlerComments() : "";
                comments += "\nLa póliza asociada a esta reclamación ha sido cancelada: " + policyNumber;
                claim.setHandlerComments(comments);

                claimRepository.save(claim);

                log.info("Actualizada reclamación {} con nota sobre póliza cancelada", claim.getClaimNumber());
            }
        } catch (Exception e) {
            log.error("Error procesando evento de póliza cancelada", e);
        }
    }

    private boolean isOpenClaim(Claim.ClaimStatus status) {
        return status != Claim.ClaimStatus.CLOSED &&
                status != Claim.ClaimStatus.PAID &&
                status != Claim.ClaimStatus.CANCELLED;
    }
}
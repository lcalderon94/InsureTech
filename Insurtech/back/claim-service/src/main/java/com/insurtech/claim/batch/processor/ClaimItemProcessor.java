package com.insurtech.claim.batch.processor;

import com.insurtech.claim.model.entity.Claim;
import com.insurtech.claim.model.entity.ClaimItem;
import com.insurtech.claim.repository.ClaimItemRepository;
import com.insurtech.claim.repository.ClaimRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class ClaimItemProcessor implements ItemProcessor<Claim, Claim> {

    private static final Logger log = LoggerFactory.getLogger(ClaimItemProcessor.class);

    @Autowired
    private ClaimRepository claimRepository;

    @Autowired
    private ClaimItemRepository claimItemRepository;

    @Override
    public Claim process(Claim claim) throws Exception {
        log.info("Procesando reclamación: {}", claim.getClaimNumber());

        try {
            // Cargar la reclamación completa desde la base de datos
            Claim fullClaim = claimRepository.findById(claim.getId())
                    .orElseThrow(() -> new RuntimeException("Reclamación no encontrada: " + claim.getId()));

            // Cargar los ítems de la reclamación
            List<ClaimItem> items = claimItemRepository.findByClaimId(claim.getId());
            log.debug("Reclamación {} tiene {} ítems", claim.getClaimNumber(), items.size());

            // Si la reclamación está en estado SUBMITTED, pasarla a UNDER_REVIEW
            if (fullClaim.getStatus() == Claim.ClaimStatus.SUBMITTED) {
                fullClaim.setStatus(Claim.ClaimStatus.UNDER_REVIEW);
                fullClaim.setUpdatedAt(LocalDateTime.now());
                fullClaim.setUpdatedBy("batch-processor");
                log.info("Reclamación {} cambiada a estado UNDER_REVIEW", claim.getClaimNumber());
            }

            // Si la reclamación ha estado en UNDER_REVIEW por más de 7 días, marcarla para revisión
            if (fullClaim.getStatus() == Claim.ClaimStatus.UNDER_REVIEW) {
                LocalDateTime updatedAt = fullClaim.getUpdatedAt();
                if (updatedAt != null && updatedAt.plusDays(7).isBefore(LocalDateTime.now())) {
                    fullClaim.setHandlerComments(fullClaim.getHandlerComments() +
                            "\nMarcado para revisión prioritaria por el sistema: " + LocalDateTime.now());
                    fullClaim.setUpdatedAt(LocalDateTime.now());
                    fullClaim.setUpdatedBy("batch-processor");
                    log.info("Reclamación {} marcada para revisión prioritaria", claim.getClaimNumber());
                }
            }

            // Recalcular montos si es necesario
            if (items != null && !items.isEmpty()) {
                // Calcular monto estimado basado en ítems
                BigDecimal totalEstimated = BigDecimal.ZERO;
                for (ClaimItem item : items) {
                    if (item.getClaimedAmount() != null) {
                        totalEstimated = totalEstimated.add(item.getClaimedAmount());
                    }
                }

                // Actualizar monto estimado si hay cambios
                if (fullClaim.getEstimatedAmount() == null ||
                        !fullClaim.getEstimatedAmount().equals(totalEstimated)) {
                    fullClaim.setEstimatedAmount(totalEstimated);
                    log.info("Monto estimado recalculado para reclamación {}: {}",
                            claim.getClaimNumber(), totalEstimated);
                }
            }

            return fullClaim;
        } catch (Exception e) {
            log.error("Error al procesar reclamación: " + claim.getClaimNumber(), e);
            throw e;
        }
    }
}
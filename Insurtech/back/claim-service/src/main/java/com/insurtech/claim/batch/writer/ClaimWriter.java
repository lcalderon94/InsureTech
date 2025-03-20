package com.insurtech.claim.batch.writer;

import com.insurtech.claim.model.entity.Claim;
import com.insurtech.claim.model.entity.ClaimStatusHistory;
import com.insurtech.claim.repository.ClaimRepository;
import com.insurtech.claim.repository.ClaimStatusHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ClaimWriter implements ItemWriter<Claim> {

    private static final Logger log = LoggerFactory.getLogger(ClaimWriter.class);

    @Autowired
    private ClaimRepository claimRepository;

    @Autowired
    private ClaimStatusHistoryRepository statusHistoryRepository;

    @Override
    public void write(Chunk<? extends Claim> chunk) throws Exception {
        List<Claim> claims = new ArrayList<>(chunk.getItems());
        log.info("Escribiendo lote de {} reclamaciones", claims.size());

        List<ClaimStatusHistory> statusHistories = new ArrayList<>();

        // Para cada reclamación, verificar si cambió el estado y crear historial
        for (Claim claim : claims) {
            Claim existingClaim = claimRepository.findById(claim.getId()).orElse(null);

            // Si existe y ha cambiado el estado, crear historial
            if (existingClaim != null && existingClaim.getStatus() != claim.getStatus()) {
                ClaimStatusHistory statusHistory = new ClaimStatusHistory();
                statusHistory.setClaim(claim);
                statusHistory.setPreviousStatus(existingClaim.getStatus());
                statusHistory.setNewStatus(claim.getStatus());
                statusHistory.setChangeReason("Actualización automática por procesamiento por lotes");
                statusHistory.setCreatedBy("batch-processor");
                statusHistories.add(statusHistory);

                log.info("Cambio de estado detectado para reclamación {}: {} -> {}",
                        claim.getClaimNumber(), existingClaim.getStatus(), claim.getStatus());
            }
        }

        // Guardar todas las reclamaciones en un único batch
        claimRepository.saveAll(claims);
        log.info("Guardadas {} reclamaciones", claims.size());

        // Guardar historiales de estado si hay
        if (!statusHistories.isEmpty()) {
            statusHistoryRepository.saveAll(statusHistories);
            log.info("Guardados {} registros de historial de estado", statusHistories.size());
        }
    }
}
package com.insurtech.claim.service;

import com.insurtech.claim.model.dto.BatchProcessingRequestDto;
import com.insurtech.claim.model.entity.Claim;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface ClaimBatchService {

    /**
     * Inicia un procesamiento por lotes de reclamaciones
     */
    String startBatchProcessing(BatchProcessingRequestDto request);

    /**
     * Procesa reclamaciones desde un archivo CSV
     */
    String processClaimsFromCsv(InputStream inputStream);

    /**
     * Actualiza el estado de múltiples reclamaciones en lote
     */
    String batchStatusUpdate(BatchProcessingRequestDto request);

    /**
     * Exporta reclamaciones a un formato específico (CSV, Excel, etc.)
     */
    CompletableFuture<byte[]> exportClaimsAsync(
            List<String> claimNumbers,
            List<Claim.ClaimStatus> statuses,
            LocalDate startDate,
            LocalDate endDate,
            String format);

    /**
     * Consulta el estado de un proceso por lotes
     */
    Map<String, Object> getBatchStatus(String batchId);

    /**
     * Calcula estadísticas sobre un conjunto de reclamaciones
     */
    CompletableFuture<Map<String, Object>> calculateBatchStatisticsAsync(
            List<String> claimNumbers,
            List<Claim.ClaimStatus> statuses,
            LocalDate startDate,
            LocalDate endDate);

    /**
     * Asigna múltiples reclamaciones a un tramitador
     */
    String massAssignClaims(BatchProcessingRequestDto request, String assignedTo);

    /**
     * Genera un informe de rendimiento de reclamaciones
     */
    CompletableFuture<Map<String, Object>> generatePerformanceReportAsync(
            LocalDate startDate,
            LocalDate endDate);
}
package com.insurtech.claim.controller;

import com.insurtech.claim.model.dto.BatchProcessingRequestDto;
import com.insurtech.claim.model.dto.ClaimDto;
import com.insurtech.claim.model.entity.Claim;
import com.insurtech.claim.service.ClaimBatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/claims/batch")
@Tag(name = "Procesamiento por Lotes", description = "API para operaciones por lotes sobre reclamaciones")
@SecurityRequirement(name = "bearer-jwt")
@RequiredArgsConstructor
public class ClaimBatchController {

    private static final Logger log = LoggerFactory.getLogger(ClaimBatchController.class);

    private final ClaimBatchService batchService;

    @PostMapping("/process")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Procesar lote de reclamaciones", description = "Procesa un lote de reclamaciones asíncronamente")
    public ResponseEntity<String> processBatch(@Valid @RequestBody BatchProcessingRequestDto request) {
        log.info("Iniciando procesamiento por lotes para reclamaciones");

        String batchId = batchService.startBatchProcessing(request);

        return new ResponseEntity<>("Procesamiento de lote iniciado con ID: " + batchId, HttpStatus.ACCEPTED);
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cargar CSV de reclamaciones", description = "Carga y procesa un archivo CSV con datos de reclamaciones")
    public ResponseEntity<String> uploadClaimsCsv(@RequestParam("file") MultipartFile file) {
        log.info("Cargando archivo CSV de reclamaciones: {}", file.getOriginalFilename());

        try {
            String batchId = batchService.processClaimsFromCsv(file.getInputStream());
            return new ResponseEntity<>("Carga de archivo CSV iniciada con ID de lote: " + batchId, HttpStatus.ACCEPTED);
        } catch (IOException e) {
            log.error("Error al leer el archivo CSV", e);
            return new ResponseEntity<>("Error al leer el archivo: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/status-update")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar estado de reclamaciones en lote", description = "Actualiza el estado de múltiples reclamaciones a la vez")
    public ResponseEntity<String> updateClaimStatusInBatch(@RequestBody BatchProcessingRequestDto request) {
        log.info("Actualizando estado a {} para múltiples reclamaciones", request.getTargetStatus());

        String batchId = batchService.batchStatusUpdate(request);

        return new ResponseEntity<>("Actualización masiva de estado iniciada con ID de lote: " + batchId, HttpStatus.ACCEPTED);
    }

    @GetMapping("/exports")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Exportar reclamaciones", description = "Exporta reclamaciones a un formato específico (CSV, Excel, etc.)")
    public CompletableFuture<ResponseEntity<byte[]>> exportClaims(
            @RequestParam(required = false) List<String> claimNumbers,
            @RequestParam(required = false) List<Claim.ClaimStatus> statuses,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false, defaultValue = "csv") String format) {

        log.info("Exportando reclamaciones en formato {}", format);

        return batchService.exportClaimsAsync(claimNumbers, statuses, startDate, endDate, format)
                .thenApply(data -> {
                    String contentType = "text/csv";
                    String filename = "claims-export.csv";

                    if ("excel".equalsIgnoreCase(format)) {
                        contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                        filename = "claims-export.xlsx";
                    }

                    return ResponseEntity.ok()
                            .contentType(MediaType.parseMediaType(contentType))
                            .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                            .body(data);
                });
    }

    @GetMapping("/status/{batchId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Consultar estado de lote", description = "Consulta el estado de un proceso por lotes")
    public ResponseEntity<Map<String, Object>> getBatchStatus(@PathVariable String batchId) {
        log.info("Consultando estado del lote: {}", batchId);

        Map<String, Object> status = batchService.getBatchStatus(batchId);

        return ResponseEntity.ok(status);
    }

    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Estadísticas de reclamaciones", description = "Calcula estadísticas sobre un conjunto de reclamaciones")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> calculateBatchStatistics(
            @RequestParam(required = false) List<String> claimNumbers,
            @RequestParam(required = false) List<Claim.ClaimStatus> statuses,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("Calculando estadísticas para un conjunto de reclamaciones");

        return batchService.calculateBatchStatisticsAsync(claimNumbers, statuses, startDate, endDate)
                .thenApply(ResponseEntity::ok);
    }

    @PostMapping("/mass-assign")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Asignación masiva", description = "Asigna múltiples reclamaciones a un tramitador")
    public ResponseEntity<String> massAssignClaims(
            @RequestBody BatchProcessingRequestDto request,
            @RequestParam String assignedTo) {

        log.info("Asignando reclamaciones al tramitador: {}", assignedTo);

        String batchId = batchService.massAssignClaims(request, assignedTo);

        return new ResponseEntity<>("Asignación masiva iniciada con ID de lote: " + batchId, HttpStatus.ACCEPTED);
    }

    @GetMapping("/report/performance")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Informe de rendimiento", description = "Genera un informe de rendimiento de reclamaciones")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> getPerformanceReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("Generando informe de rendimiento de reclamaciones");

        return batchService.generatePerformanceReportAsync(startDate, endDate)
                .thenApply(ResponseEntity::ok);
    }
}
package com.insurtech.claim.service.impl;

import com.insurtech.claim.exception.BusinessValidationException;
import com.insurtech.claim.exception.ResourceNotFoundException;
import com.insurtech.claim.model.dto.BatchProcessingRequestDto;
import com.insurtech.claim.model.dto.ClaimDto;
import com.insurtech.claim.model.entity.Claim;
import com.insurtech.claim.model.entity.ClaimStatusHistory;
import com.insurtech.claim.repository.ClaimRepository;
import com.insurtech.claim.repository.ClaimStatusHistoryRepository;
import com.insurtech.claim.service.ClaimBatchService;
import com.insurtech.claim.service.ClaimService;
import com.insurtech.claim.util.EntityDtoMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClaimBatchServiceImpl implements ClaimBatchService {

    private static final Logger log = LoggerFactory.getLogger(ClaimBatchServiceImpl.class);

    private final ClaimRepository claimRepository;
    private final ClaimStatusHistoryRepository statusHistoryRepository;
    private final ClaimService claimService;
    private final EntityDtoMapper mapper;
    @Autowired(required = false)
    private JobLauncher jobLauncher;

    @Autowired(required = false)
    private Job processClaimsJob;

    // Almacén en memoria para seguimiento de estado de batch (en producción sería recomendable usar Redis)
    private final Map<String, Map<String, Object>> batchStatus = new ConcurrentHashMap<>();

    @Override
    @Async
    @Transactional
    public String startBatchProcessing(BatchProcessingRequestDto request) {
        log.info("Iniciando procesamiento por lotes");

        // Generar ID de lote
        String batchId = UUID.randomUUID().toString();

        // Inicializar estado del lote
        Map<String, Object> status = new HashMap<>();
        status.put("id", batchId);
        status.put("startTime", LocalDateTime.now());
        status.put("status", "PROCESSING");
        status.put("totalClaims", 0);
        status.put("processedClaims", 0);
        status.put("successfulClaims", 0);
        status.put("failedClaims", 0);
        batchStatus.put(batchId, status);

        try {
            // Lanzar job asíncrono si está disponible
            if (jobLauncher != null && processClaimsJob != null) {
                JobParameters jobParameters = new JobParametersBuilder()
                        .addString("batchId", batchId)
                        .addLong("time", System.currentTimeMillis())
                        .toJobParameters();

                JobExecution execution = jobLauncher.run(processClaimsJob, jobParameters);
                status.put("jobExecutionId", execution.getId());

                log.info("Job lanzado con ID: {}", execution.getId());
            } else {
                log.info("Procesando en modo alternativo sin Spring Batch");
                // Procesamiento alternativo si Spring Batch no está configurado
                processBatchAlternative(request, batchId, status);
            }
        } catch (Exception e) {
            log.error("Error al lanzar job de procesamiento por lotes", e);
            status.put("status", "FAILED");
            status.put("error", e.getMessage());
        }

        return batchId;
    }

    @Override
    @Async
    @Transactional
    public String processClaimsFromCsv(InputStream inputStream) {
        log.info("Procesando reclamaciones desde CSV");

        // Generar ID de lote
        String batchId = UUID.randomUUID().toString();

        // Inicializar estado del lote
        Map<String, Object> status = new HashMap<>();
        status.put("id", batchId);
        status.put("startTime", LocalDateTime.now());
        status.put("status", "PROCESSING");
        status.put("totalClaims", 0);
        status.put("processedClaims", 0);
        status.put("successfulClaims", 0);
        status.put("failedClaims", 0);
        status.put("source", "CSV");
        batchStatus.put(batchId, status);

        CompletableFuture.runAsync(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                // Leer encabezados
                String headerLine = reader.readLine();
                if (headerLine == null) {
                    throw new BusinessValidationException("El archivo CSV está vacío");
                }

                List<String> headers = Arrays.asList(headerLine.split(","));

                // Leer y procesar líneas
                String line;
                int lineCount = 0;
                int successCount = 0;
                int failCount = 0;

                while ((line = reader.readLine()) != null) {
                    lineCount++;

                    try {
                        String[] values = line.split(",");

                        // Mapear valores a DTO
                        ClaimDto claimDto = new ClaimDto();

                        for (int i = 0; i < headers.size() && i < values.length; i++) {
                            String header = headers.get(i);
                            String value = values[i].trim();

                            switch (header.toLowerCase()) {
                                case "policynumber":
                                    claimDto.setPolicyNumber(value);
                                    break;
                                case "customernumber":
                                    claimDto.setCustomerNumber(value);
                                    break;
                                case "incidentdate":
                                    if (!value.isEmpty()) {
                                        claimDto.setIncidentDate(LocalDate.parse(value));
                                    }
                                    break;
                                case "incidentdescription":
                                    claimDto.setIncidentDescription(value);
                                    break;
                                case "claimtype":
                                    if (!value.isEmpty()) {
                                        claimDto.setClaimType(Claim.ClaimType.valueOf(value));
                                    }
                                    break;
                                case "estimatedamount":
                                    if (!value.isEmpty()) {
                                        claimDto.setEstimatedAmount(new java.math.BigDecimal(value));
                                    }
                                    break;
                                // Mapear otros campos según sea necesario
                            }
                        }

                        // Validar campos obligatorios
                        if (claimDto.getIncidentDate() == null) {
                            throw new BusinessValidationException("Fecha de incidente es obligatoria");
                        }
                        if (claimDto.getIncidentDescription() == null || claimDto.getIncidentDescription().isEmpty()) {
                            throw new BusinessValidationException("Descripción de incidente es obligatoria");
                        }

                        // Crear reclamación
                        claimService.createClaim(claimDto);
                        successCount++;

                    } catch (Exception e) {
                        log.error("Error al procesar línea {} del CSV: {}", lineCount, e.getMessage());
                        failCount++;
                    }

                    // Actualizar estado
                    status.put("totalClaims", lineCount);
                    status.put("processedClaims", successCount + failCount);
                    status.put("successfulClaims", successCount);
                    status.put("failedClaims", failCount);
                }

                status.put("endTime", LocalDateTime.now());
                status.put("status", "COMPLETED");

            } catch (Exception e) {
                log.error("Error al procesar archivo CSV", e);
                status.put("status", "FAILED");
                status.put("error", e.getMessage());
                status.put("endTime", LocalDateTime.now());
            }
        });

        return batchId;
    }

    @Override
    @Async
    @Transactional
    public String batchStatusUpdate(BatchProcessingRequestDto request) {
        log.info("Actualizando estado de reclamaciones en lote");

        // Validar solicitud
        if (request.getTargetStatus() == null) {
            throw new BusinessValidationException("Se requiere el estado objetivo");
        }

        List<Claim> claimsToUpdate = new ArrayList<>();

        // Buscar por ID
        if (request.getClaimIds() != null && !request.getClaimIds().isEmpty()) {
            claimRepository.findAllById(request.getClaimIds())
                    .forEach(claimsToUpdate::add);
        }

        // Buscar por número
        if (request.getClaimNumbers() != null && !request.getClaimNumbers().isEmpty()) {
            for (String number : request.getClaimNumbers()) {
                claimRepository.findByClaimNumber(number)
                        .ifPresent(claimsToUpdate::add);
            }
        }

        // Buscar por estado
        if (request.getStatuses() != null && !request.getStatuses().isEmpty()) {
            for (Claim.ClaimStatus status : request.getStatuses()) {
                claimsToUpdate.addAll(claimRepository.findByStatus(status));
            }
        }

        // Buscar por rango de fechas
        if (request.getIncidentDateFrom() != null && request.getIncidentDateTo() != null) {
            claimsToUpdate.addAll(claimRepository.findByIncidentDateBetween(
                    request.getIncidentDateFrom(), request.getIncidentDateTo()));
        }

        // Generar ID de lote
        String batchId = UUID.randomUUID().toString();

        // Inicializar estado del lote
        Map<String, Object> status = new HashMap<>();
        status.put("id", batchId);
        status.put("startTime", LocalDateTime.now());
        status.put("status", "PROCESSING");
        status.put("totalClaims", claimsToUpdate.size());
        status.put("processedClaims", 0);
        status.put("successfulClaims", 0);
        status.put("failedClaims", 0);
        status.put("targetStatus", request.getTargetStatus().name());
        batchStatus.put(batchId, status);

        // Procesar asíncronamente
        CompletableFuture.runAsync(() -> {
            int successCount = 0;
            int failCount = 0;

            for (Claim claim : claimsToUpdate) {
                try {
                    // Actualizar estado
                    Claim.ClaimStatus oldStatus = claim.getStatus();
                    claim.setStatus(request.getTargetStatus());
                    claim.setUpdatedBy(getCurrentUsername());
                    claim.setUpdatedAt(LocalDateTime.now());

                    // Guardar historial de cambio de estado
                    ClaimStatusHistory statusHistory = new ClaimStatusHistory();
                    statusHistory.setClaim(claim);
                    statusHistory.setPreviousStatus(oldStatus);
                    statusHistory.setNewStatus(request.getTargetStatus());
                    statusHistory.setChangeReason(request.getProcessingReason());
                    statusHistory.setCreatedBy(getCurrentUsername());

                    claimRepository.save(claim);
                    statusHistoryRepository.save(statusHistory);

                    successCount++;
                } catch (Exception e) {
                    log.error("Error al actualizar reclamación ID {}: {}", claim.getId(), e.getMessage());
                    failCount++;
                }

                // Actualizar estado del lote
                status.put("processedClaims", successCount + failCount);
                status.put("successfulClaims", successCount);
                status.put("failedClaims", failCount);
            }

            status.put("endTime", LocalDateTime.now());
            status.put("status", "COMPLETED");
        });

        return batchId;
    }

    @Override
    @Async
    public CompletableFuture<byte[]> exportClaimsAsync(List<String> claimNumbers, List<Claim.ClaimStatus> statuses,
                                                       LocalDate startDate, LocalDate endDate, String format) {
        log.info("Exportando reclamaciones en formato {}", format);

        return CompletableFuture.supplyAsync(() -> {
            try {
                // Obtener reclamaciones según criterios
                List<Claim> claims = new ArrayList<>();

                if (claimNumbers != null && !claimNumbers.isEmpty()) {
                    for (String number : claimNumbers) {
                        claimRepository.findByClaimNumber(number)
                                .ifPresent(claims::add);
                    }
                } else {
                    // Usar otros criterios de búsqueda
                    if (statuses != null && !statuses.isEmpty()) {
                        for (Claim.ClaimStatus status : statuses) {
                            claims.addAll(claimRepository.findByStatus(status));
                        }
                    }

                    if (startDate != null && endDate != null) {
                        claims.addAll(claimRepository.findByIncidentDateBetween(startDate, endDate));
                    }

                    if ((statuses == null || statuses.isEmpty()) &&
                            (startDate == null || endDate == null)) {
                        // Sin criterios, usar un límite por defecto
                        claims = claimRepository.findAll().stream()
                                .limit(1000)  // Limitar para evitar problemas de memoria
                                .collect(Collectors.toList());
                    }
                }

                // Generar el archivo según el formato solicitado
                if ("excel".equalsIgnoreCase(format)) {
                    return generateExcelExport(claims);
                } else {
                    // Por defecto, generar CSV
                    return generateCsvExport(claims);
                }

            } catch (Exception e) {
                log.error("Error al exportar reclamaciones", e);
                throw new RuntimeException("Error al exportar reclamaciones: " + e.getMessage());
            }
        });
    }

    @Override
    public Map<String, Object> getBatchStatus(String batchId) {
        log.info("Consultando estado del lote: {}", batchId);

        Map<String, Object> status = batchStatus.get(batchId);
        if (status == null) {
            throw new ResourceNotFoundException("No se encontró información para el lote con ID: " + batchId);
        }

        return new HashMap<>(status);  // Devolver copia para evitar modificaciones externas
    }

    @Override
    @Async
    public CompletableFuture<Map<String, Object>> calculateBatchStatisticsAsync(List<String> claimNumbers,
                                                                                List<Claim.ClaimStatus> statuses,
                                                                                LocalDate startDate,
                                                                                LocalDate endDate) {
        log.info("Calculando estadísticas para un conjunto de reclamaciones");

        return CompletableFuture.supplyAsync(() -> {
            Map<String, Object> statistics = new HashMap<>();

            try {
                // Obtener reclamaciones según criterios
                List<Claim> claims = new ArrayList<>();

                if (claimNumbers != null && !claimNumbers.isEmpty()) {
                    for (String number : claimNumbers) {
                        claimRepository.findByClaimNumber(number)
                                .ifPresent(claims::add);
                    }
                } else {
                    // Usar otros criterios de búsqueda
                    if (statuses != null && !statuses.isEmpty()) {
                        for (Claim.ClaimStatus status : statuses) {
                            claims.addAll(claimRepository.findByStatus(status));
                        }
                    }

                    if (startDate != null && endDate != null) {
                        claims.addAll(claimRepository.findByIncidentDateBetween(startDate, endDate));
                    }
                }

                // Calcular estadísticas
                statistics.put("totalClaims", claims.size());

                // Recuento por estado
                Map<Claim.ClaimStatus, Long> countByStatus = new HashMap<>();
                for (Claim.ClaimStatus status : Claim.ClaimStatus.values()) {
                    long count = claims.stream()
                            .filter(c -> c.getStatus() == status)
                            .count();
                    countByStatus.put(status, count);
                }
                statistics.put("countByStatus", countByStatus);

                // Recuento por tipo
                Map<Claim.ClaimType, Long> countByType = new HashMap<>();
                for (Claim.ClaimType type : Claim.ClaimType.values()) {
                    long count = claims.stream()
                            .filter(c -> c.getClaimType() == type)
                            .count();
                    countByType.put(type, count);
                }
                statistics.put("countByType", countByType);

                // Montos totales
                java.math.BigDecimal totalEstimated = claims.stream()
                        .filter(c -> c.getEstimatedAmount() != null)
                        .map(Claim::getEstimatedAmount)
                        .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
                statistics.put("totalEstimatedAmount", totalEstimated);

                java.math.BigDecimal totalApproved = claims.stream()
                        .filter(c -> c.getApprovedAmount() != null)
                        .map(Claim::getApprovedAmount)
                        .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
                statistics.put("totalApprovedAmount", totalApproved);

                java.math.BigDecimal totalPaid = claims.stream()
                        .filter(c -> c.getPaidAmount() != null)
                        .map(Claim::getPaidAmount)
                        .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
                statistics.put("totalPaidAmount", totalPaid);

                // Fecha calculada
                statistics.put("calculatedAt", LocalDateTime.now());

            } catch (Exception e) {
                log.error("Error al calcular estadísticas", e);
                statistics.put("error", "Error al calcular estadísticas: " + e.getMessage());
            }

            return statistics;
        });
    }

    @Override
    @Async
    @Transactional
    public String massAssignClaims(BatchProcessingRequestDto request, String assignedTo) {
        log.info("Asignando reclamaciones al tramitador: {}", assignedTo);

        if (assignedTo == null || assignedTo.trim().isEmpty()) {
            throw new BusinessValidationException("Se requiere especificar un tramitador");
        }

        List<Claim> claimsToAssign = new ArrayList<>();

        // Buscar por ID
        if (request.getClaimIds() != null && !request.getClaimIds().isEmpty()) {
            claimRepository.findAllById(request.getClaimIds())
                    .forEach(claimsToAssign::add);
        }

        // Buscar por número
        if (request.getClaimNumbers() != null && !request.getClaimNumbers().isEmpty()) {
            for (String number : request.getClaimNumbers()) {
                claimRepository.findByClaimNumber(number)
                        .ifPresent(claimsToAssign::add);
            }
        }

        // Buscar por estado
        if (request.getStatuses() != null && !request.getStatuses().isEmpty()) {
            for (Claim.ClaimStatus status : request.getStatuses()) {
                claimsToAssign.addAll(claimRepository.findByStatus(status));
            }
        }

        // Buscar por rango de fechas
        if (request.getIncidentDateFrom() != null && request.getIncidentDateTo() != null) {
            claimsToAssign.addAll(claimRepository.findByIncidentDateBetween(
                    request.getIncidentDateFrom(), request.getIncidentDateTo()));
        }

        // Generar ID de lote
        String batchId = UUID.randomUUID().toString();

        // Inicializar estado del lote
        Map<String, Object> status = new HashMap<>();
        status.put("id", batchId);
        status.put("startTime", LocalDateTime.now());
        status.put("status", "PROCESSING");
        status.put("totalClaims", claimsToAssign.size());
        status.put("assignedTo", assignedTo);
        status.put("processedClaims", 0);
        status.put("successfulClaims", 0);
        status.put("failedClaims", 0);
        batchStatus.put(batchId, status);

        // Procesar asíncronamente
        CompletableFuture.runAsync(() -> {
            int successCount = 0;
            int failCount = 0;

            for (Claim claim : claimsToAssign) {
                try {
                    // Actualizar comentarios con información de asignación
                    String comments = claim.getHandlerComments();
                    String assignmentNote = "Reclamación asignada a " + assignedTo + " el "
                            + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

                    if (comments == null || comments.isEmpty()) {
                        claim.setHandlerComments(assignmentNote);
                    } else {
                        claim.setHandlerComments(comments + "\n" + assignmentNote);
                    }

                    // Actualizar campo de updatedBy
                    claim.setUpdatedBy(getCurrentUsername());
                    claim.setUpdatedAt(LocalDateTime.now());

                    claimRepository.save(claim);

                    successCount++;
                } catch (Exception e) {
                    log.error("Error al asignar reclamación ID {}: {}", claim.getId(), e.getMessage());
                    failCount++;
                }

                // Actualizar estado del lote
                status.put("processedClaims", successCount + failCount);
                status.put("successfulClaims", successCount);
                status.put("failedClaims", failCount);
            }

            status.put("endTime", LocalDateTime.now());
            status.put("status", "COMPLETED");
        });

        return batchId;
    }

    @Override
    @Async
    public CompletableFuture<Map<String, Object>> generatePerformanceReportAsync(LocalDate startDate, LocalDate endDate) {
        log.info("Generando informe de rendimiento de reclamaciones");

        return CompletableFuture.supplyAsync(() -> {
            Map<String, Object> report = new HashMap<>();

            try {
                // Usar fechas por defecto si no se proporcionan
                LocalDate start = startDate != null ? startDate : LocalDate.now().minusMonths(3);
                LocalDate end = endDate != null ? endDate : LocalDate.now();

                report.put("reportPeriod", Map.of(
                        "startDate", start,
                        "endDate", end
                ));

                // Obtener reclamaciones en el período
                List<Claim> claims = claimRepository.findByIncidentDateBetween(start, end);

                // Número total de reclamaciones
                report.put("totalClaims", claims.size());

                // Tiempo medio de resolución (días)
                double avgResolutionDays = claims.stream()
                        .filter(c -> c.getSubmissionDate() != null && c.getSettlementDate() != null)
                        .mapToLong(c -> java.time.temporal.ChronoUnit.DAYS.between(
                                c.getSubmissionDate(), c.getSettlementDate()))
                        .average()
                        .orElse(0);
                report.put("avgResolutionDays", avgResolutionDays);

                // Tasa de aprobación
                long approvedClaims = claims.stream()
                        .filter(c -> c.getStatus() == Claim.ClaimStatus.APPROVED ||
                                c.getStatus() == Claim.ClaimStatus.PARTIALLY_APPROVED ||
                                c.getStatus() == Claim.ClaimStatus.PAID ||
                                c.getStatus() == Claim.ClaimStatus.PARTIALLY_PAID)
                        .count();
                double approvalRate = claims.isEmpty() ? 0 : (double) approvedClaims / claims.size() * 100;
                report.put("approvalRate", approvalRate);

                // Monto medio de reclamación
                double avgClaimAmount = claims.stream()
                        .filter(c -> c.getEstimatedAmount() != null)
                        .mapToDouble(c -> c.getEstimatedAmount().doubleValue())
                        .average()
                        .orElse(0);
                report.put("avgClaimAmount", avgClaimAmount);

                // Distribución por tipo de reclamación
                Map<Claim.ClaimType, Long> claimsByType = new HashMap<>();
                Arrays.stream(Claim.ClaimType.values()).forEach(type -> {
                    long count = claims.stream()
                            .filter(c -> c.getClaimType() == type)
                            .count();
                    claimsByType.put(type, count);
                });
                report.put("claimsByType", claimsByType);

                // Tendencia mensual
                Map<String, Long> monthlyTrend = claims.stream()
                        .collect(Collectors.groupingBy(
                                c -> c.getIncidentDate().format(DateTimeFormatter.ofPattern("yyyy-MM")),
                                Collectors.counting()
                        ));
                report.put("monthlyTrend", monthlyTrend);

                // Fecha del informe
                report.put("generatedAt", LocalDateTime.now());

            } catch (Exception e) {
                log.error("Error al generar informe de rendimiento", e);
                report.put("error", "Error al generar informe: " + e.getMessage());
            }

            return report;
        });
    }

    // Método alternativo para procesar lotes sin Spring Batch
    private void processBatchAlternative(BatchProcessingRequestDto request, String batchId, Map<String, Object> status) {
        CompletableFuture.runAsync(() -> {
            try {
                List<Claim> claimsToProcess = new ArrayList<>();

                // Buscar por ID
                if (request.getClaimIds() != null && !request.getClaimIds().isEmpty()) {
                    claimRepository.findAllById(request.getClaimIds())
                            .forEach(claimsToProcess::add);
                }

                // Buscar por número
                if (request.getClaimNumbers() != null && !request.getClaimNumbers().isEmpty()) {
                    for (String number : request.getClaimNumbers()) {
                        claimRepository.findByClaimNumber(number)
                                .ifPresent(claimsToProcess::add);
                    }
                }

                // Buscar por estado
                if (request.getStatuses() != null && !request.getStatuses().isEmpty()) {
                    for (Claim.ClaimStatus claimStatus : request.getStatuses()) {
                        claimsToProcess.addAll(claimRepository.findByStatus(claimStatus));
                    }
                }

                // Procesar reclamaciones
                status.put("totalClaims", claimsToProcess.size());
                int processed = 0;
                int successful = 0;
                int failed = 0;

                for (Claim claim : claimsToProcess) {
                    try {
                        // Procesar reclamación (lógica básica de ejemplo)
                        if (claim.getStatus() == Claim.ClaimStatus.SUBMITTED) {
                            claim.setStatus(Claim.ClaimStatus.UNDER_REVIEW);
                        }
                        claim.setUpdatedBy(getCurrentUsername());
                        claim.setUpdatedAt(LocalDateTime.now());

                        claimRepository.save(claim);
                        successful++;
                    } catch (Exception e) {
                        log.error("Error al procesar reclamación ID {}: {}", claim.getId(), e.getMessage());
                        failed++;
                    }

                    processed++;
                    status.put("processedClaims", processed);
                    status.put("successfulClaims", successful);
                    status.put("failedClaims", failed);
                }

                status.put("endTime", LocalDateTime.now());
                status.put("status", "COMPLETED");
            } catch (Exception e) {
                log.error("Error en procesamiento alternativo", e);
                status.put("status", "FAILED");
                status.put("error", e.getMessage());
                status.put("endTime", LocalDateTime.now());
            }
        });
    }

    // Métodos auxiliares privados

    private String getCurrentUsername() {
        try {
            return SecurityContextHolder.getContext().getAuthentication().getName();
        } catch (Exception e) {
            return "system";
        }
    }

    private byte[] generateCsvExport(List<Claim> claims) {
        StringBuilder csv = new StringBuilder();

        // Encabezados
        csv.append("ClaimNumber,PolicyNumber,CustomerNumber,IncidentDate,ClaimType,Status,")
                .append("EstimatedAmount,ApprovedAmount,PaidAmount,SubmissionDate,SettlementDate\n");

        // Datos
        DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE;
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

        for (Claim claim : claims) {
            csv.append(claim.getClaimNumber()).append(",")
                    .append(claim.getPolicyNumber()).append(",")
                    .append(claim.getCustomerNumber()).append(",")
                    .append(claim.getIncidentDate().format(dateFormatter)).append(",")
                    .append(claim.getClaimType()).append(",")
                    .append(claim.getStatus()).append(",")
                    .append(claim.getEstimatedAmount()).append(",")
                    .append(claim.getApprovedAmount()).append(",")
                    .append(claim.getPaidAmount()).append(",")
                    .append(claim.getSubmissionDate() != null ? claim.getSubmissionDate().format(dateTimeFormatter) : "").append(",")
                    .append(claim.getSettlementDate() != null ? claim.getSettlementDate().format(dateTimeFormatter) : "")
                    .append("\n");
        }

        return csv.toString().getBytes();
    }

    private byte[] generateExcelExport(List<Claim> claims) {
        // En una implementación real, utilizaríamos Apache POI para generar Excel
        // Para este ejemplo, devolvemos un CSV básico como alternativa
        return generateCsvExport(claims);
    }
}
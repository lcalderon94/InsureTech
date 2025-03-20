package com.insurtech.claim.service.impl;

import com.insurtech.claim.client.CustomerClient;
import com.insurtech.claim.client.PolicyClient;
import com.insurtech.claim.event.producer.ClaimEventProducer;
import com.insurtech.claim.exception.BusinessValidationException;
import com.insurtech.claim.exception.ClaimNotFoundException;
import com.insurtech.claim.exception.ResourceNotFoundException;
import com.insurtech.claim.model.dto.ClaimDto;
import com.insurtech.claim.model.dto.ClaimItemDto;
import com.insurtech.claim.model.dto.ClaimSearchRequestDto;
import com.insurtech.claim.model.dto.ClaimSummaryDto;
import com.insurtech.claim.model.entity.Claim;
import com.insurtech.claim.model.entity.ClaimItem;
import com.insurtech.claim.model.entity.ClaimStatusHistory;
import com.insurtech.claim.repository.ClaimItemRepository;
import com.insurtech.claim.repository.ClaimRepository;
import com.insurtech.claim.repository.ClaimStatusHistoryRepository;
import com.insurtech.claim.service.ClaimService;
import com.insurtech.claim.util.EntityDtoMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClaimServiceImpl implements ClaimService {

    private static final Logger log = LoggerFactory.getLogger(ClaimServiceImpl.class);

    private final ClaimRepository claimRepository;
    private final ClaimItemRepository claimItemRepository;
    private final ClaimStatusHistoryRepository statusHistoryRepository;
    private final EntityDtoMapper mapper;
    private final ClaimEventProducer eventProducer;
    private final CustomerClient customerClient;
    private final PolicyClient policyClient;

    @Override
    @Transactional
    public ClaimDto createClaim(ClaimDto claimDto) {
        log.info("Creando reclamación para cliente");

        // Validar y obtener IDs de cliente y póliza
        Long customerId = resolveCustomerId(claimDto);
        Long policyId = resolvePolicyId(claimDto);

        claimDto.setCustomerId(customerId);
        claimDto.setPolicyId(policyId);

        // Generar número de reclamación si no se proporcionó
        if (claimDto.getClaimNumber() == null) {
            claimDto.setClaimNumber(generateClaimNumber(claimDto.getClaimType()));
        }

        // Establecer estado inicial si no se proporcionó
        if (claimDto.getStatus() == null) {
            claimDto.setStatus(Claim.ClaimStatus.SUBMITTED);
        }

        // Establecer fecha de presentación
        LocalDateTime now = LocalDateTime.now();
        claimDto.setSubmissionDate(now);

        // Crear entidad Claim
        Claim claim = mapper.toEntity(claimDto);
        claim.setCreatedBy(getCurrentUsername());
        claim.setUpdatedBy(getCurrentUsername());

        // Guardar la reclamación
        claim = claimRepository.save(claim);

        // Crear registro de historial de estado
        ClaimStatusHistory statusHistory = new ClaimStatusHistory();
        statusHistory.setClaim(claim);
        statusHistory.setNewStatus(claim.getStatus());
        statusHistory.setChangeReason("Reclamación creada");
        statusHistory.setCreatedBy(getCurrentUsername());
        statusHistoryRepository.save(statusHistory);

        // Publicar evento de reclamación creada
        eventProducer.publishClaimCreated(claim);

        log.info("Reclamación creada con éxito. ID: {}, Número: {}", claim.getId(), claim.getClaimNumber());

        return mapper.toDto(claim);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ClaimDto> getClaimById(Long id) {
        log.debug("Obteniendo reclamación por ID: {}", id);
        return claimRepository.findById(id)
                .map(mapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ClaimDto> getClaimByNumber(String claimNumber) {
        log.debug("Obteniendo reclamación por número: {}", claimNumber);
        return claimRepository.findByClaimNumber(claimNumber)
                .map(mapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ClaimDto> searchClaims(String searchTerm, Pageable pageable) {
        log.debug("Buscando reclamaciones con término: {}", searchTerm);
        return claimRepository.search(searchTerm, pageable)
                .map(mapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClaimDto> advancedSearch(ClaimSearchRequestDto searchRequest) {
        log.debug("Realizando búsqueda avanzada de reclamaciones");

        List<Claim> results = new ArrayList<>();

        // Si se proporciona número de reclamación, es búsqueda directa
        if (searchRequest.getClaimNumber() != null && !searchRequest.getClaimNumber().isEmpty()) {
            claimRepository.findByClaimNumber(searchRequest.getClaimNumber())
                    .ifPresent(results::add);
            return results.stream().map(mapper::toDto).collect(Collectors.toList());
        }

        // Si se proporciona número de póliza
        if (searchRequest.getPolicyNumber() != null && !searchRequest.getPolicyNumber().isEmpty()) {
            results = claimRepository.findByPolicyNumber(searchRequest.getPolicyNumber());
        }
        // Si se proporciona número de cliente
        else if (searchRequest.getCustomerNumber() != null && !searchRequest.getCustomerNumber().isEmpty()) {
            results = claimRepository.findByCustomerNumber(searchRequest.getCustomerNumber());
        }
        // Si se proporciona email de cliente
        else if (searchRequest.getCustomerEmail() != null && !searchRequest.getCustomerEmail().isEmpty()) {
            results = getClaimsByCustomerEmail(searchRequest.getCustomerEmail())
                    .stream()
                    .map(mapper::toEntity)
                    .collect(Collectors.toList());
        }
        // Si se proporciona identificación de cliente
        else if (searchRequest.getIdentificationNumber() != null && searchRequest.getIdentificationType() != null) {
            try {
                Map<String, Object> customer = customerClient.getCustomerByIdentification(
                        searchRequest.getIdentificationNumber(), searchRequest.getIdentificationType());
                String customerNumber = (String) customer.get("customerNumber");
                results = claimRepository.findByCustomerNumber(customerNumber);
            } catch (Exception e) {
                log.error("Error al buscar cliente por identificación", e);
                // Continuar con lista vacía
            }
        }
        // Si no hay criterios específicos, usar los filtros de fecha y estado
        else {
            LocalDate from = searchRequest.getIncidentDateFrom();
            LocalDate to = searchRequest.getIncidentDateTo();

            if (from != null && to != null) {
                results = claimRepository.findByIncidentDateBetween(from, to);
            } else if (searchRequest.getStatuses() != null && !searchRequest.getStatuses().isEmpty()) {
                results = new ArrayList<>();
                for (Claim.ClaimStatus status : searchRequest.getStatuses()) {
                    results.addAll(claimRepository.findByStatus(status));
                }
            } else {
                throw new BusinessValidationException("Se requieren criterios de búsqueda");
            }
        }

        // Aplicar filtros adicionales
        List<Claim> filteredResults = results;

        // Filtrar por estado
        if (searchRequest.getStatuses() != null && !searchRequest.getStatuses().isEmpty()) {
            filteredResults = filteredResults.stream()
                    .filter(claim -> searchRequest.getStatuses().contains(claim.getStatus()))
                    .collect(Collectors.toList());
        }

        // Filtrar por tipo
        if (searchRequest.getTypes() != null && !searchRequest.getTypes().isEmpty()) {
            filteredResults = filteredResults.stream()
                    .filter(claim -> searchRequest.getTypes().contains(claim.getClaimType()))
                    .collect(Collectors.toList());
        }

        // Filtrar por rango de fechas de incidente
        if (searchRequest.getIncidentDateFrom() != null && searchRequest.getIncidentDateTo() != null) {
            filteredResults = filteredResults.stream()
                    .filter(claim -> !claim.getIncidentDate().isBefore(searchRequest.getIncidentDateFrom())
                            && !claim.getIncidentDate().isAfter(searchRequest.getIncidentDateTo()))
                    .collect(Collectors.toList());
        }

        // Filtrar por valor alto
        if (searchRequest.getHighValue() != null && searchRequest.getHighValue()) {
            BigDecimal threshold = new BigDecimal("10000.00"); // Umbral configurable
            filteredResults = filteredResults.stream()
                    .filter(claim -> claim.getEstimatedAmount() != null
                            && claim.getEstimatedAmount().compareTo(threshold) > 0)
                    .collect(Collectors.toList());
        }

        // Filtrar por días abiertos
        if (searchRequest.getDaysOpen() != null) {
            final int daysOpen = searchRequest.getDaysOpen();
            filteredResults = filteredResults.stream()
                    .filter(claim -> {
                        if (claim.getSubmissionDate() == null) return false;
                        long daysBetween = ChronoUnit.DAYS.between(claim.getSubmissionDate(), LocalDateTime.now());
                        return daysBetween >= daysOpen;
                    })
                    .collect(Collectors.toList());
        }

        return filteredResults.stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClaimDto> getClaimsByCustomerId(Long customerId) {
        log.debug("Obteniendo reclamaciones para el cliente: {}", customerId);
        return claimRepository.findByCustomerId(customerId).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClaimDto> getClaimsByCustomerNumber(String customerNumber) {
        log.debug("Obteniendo reclamaciones para el cliente número: {}", customerNumber);
        return claimRepository.findByCustomerNumber(customerNumber).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClaimDto> getClaimsByCustomerEmail(String email) {
        log.debug("Obteniendo reclamaciones para el cliente email: {}", email);
        try {
            Map<String, Object> customer = customerClient.getCustomerByEmail(email);
            String customerNumber = (String) customer.get("customerNumber");
            return getClaimsByCustomerNumber(customerNumber);
        } catch (Exception e) {
            log.error("Error al obtener cliente por email: {}", email, e);
            return Collections.emptyList();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClaimDto> getClaimsByPolicyId(Long policyId) {
        log.debug("Obteniendo reclamaciones para la póliza: {}", policyId);
        return claimRepository.findByPolicyId(policyId).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClaimDto> getClaimsByPolicyNumber(String policyNumber) {
        log.debug("Obteniendo reclamaciones para la póliza número: {}", policyNumber);
        return claimRepository.findByPolicyNumber(policyNumber).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ClaimDto updateClaim(Long id, ClaimDto claimDto) {
        log.info("Actualizando reclamación con ID: {}", id);

        Claim claim = claimRepository.findById(id)
                .orElseThrow(() -> new ClaimNotFoundException("Reclamación no encontrada con ID: " + id));

        // Backup del estado anterior para evento de cambio de estado
        Claim.ClaimStatus oldStatus = claim.getStatus();

        // Validar que no se pueda actualizar una reclamación cerrada o cancelada
        if (claim.getStatus() == Claim.ClaimStatus.CLOSED || claim.getStatus() == Claim.ClaimStatus.CANCELLED) {
            throw new BusinessValidationException("No se puede actualizar una reclamación " + claim.getStatus());
        }

        // Actualizar campos básicos
        if (claimDto.getIncidentDate() != null) claim.setIncidentDate(claimDto.getIncidentDate());
        if (claimDto.getIncidentDescription() != null) claim.setIncidentDescription(claimDto.getIncidentDescription());
        if (claimDto.getEstimatedAmount() != null) claim.setEstimatedAmount(claimDto.getEstimatedAmount());
        if (claimDto.getClaimType() != null) claim.setClaimType(claimDto.getClaimType());
        if (claimDto.getCustomerContactInfo() != null) claim.setCustomerContactInfo(claimDto.getCustomerContactInfo());
        if (claimDto.getHandlerComments() != null) claim.setHandlerComments(claimDto.getHandlerComments());
        if (claimDto.getStatus() != null) claim.setStatus(claimDto.getStatus());

        claim.setUpdatedBy(getCurrentUsername());

        // Guardar la reclamación actualizada
        claim = claimRepository.save(claim);

        // Crear registro de historial de estado si cambió
        if (oldStatus != claim.getStatus()) {
            ClaimStatusHistory statusHistory = new ClaimStatusHistory();
            statusHistory.setClaim(claim);
            statusHistory.setPreviousStatus(oldStatus);
            statusHistory.setNewStatus(claim.getStatus());
            statusHistory.setChangeReason("Actualización general");
            statusHistory.setCreatedBy(getCurrentUsername());
            statusHistoryRepository.save(statusHistory);

            // Publicar evento de cambio de estado
            eventProducer.publishClaimStatusChanged(claim, oldStatus);
        }

        // Publicar evento de reclamación actualizada
        eventProducer.publishClaimUpdated(claim);

        log.info("Reclamación actualizada con éxito. ID: {}", id);

        return mapper.toDto(claim);
    }

    @Override
    @Transactional
    public ClaimDto updateClaimByNumber(String claimNumber, ClaimDto claimDto) {
        log.info("Actualizando reclamación con número: {}", claimNumber);

        return claimRepository.findByClaimNumber(claimNumber)
                .map(claim -> updateClaim(claim.getId(), claimDto))
                .orElseThrow(() -> new ClaimNotFoundException("Reclamación no encontrada con número: " + claimNumber));
    }

    @Override
    @Transactional
    public ClaimDto updateClaimStatus(Long id, Claim.ClaimStatus status, String comments, BigDecimal approvedAmount, String denialReason) {
        log.info("Actualizando estado a {} para reclamación ID: {}", status, id);

        Claim claim = claimRepository.findById(id)
                .orElseThrow(() -> new ClaimNotFoundException("Reclamación no encontrada con ID: " + id));

        Claim.ClaimStatus oldStatus = claim.getStatus();

        // Validar transición de estado
        validateStatusTransition(claim.getStatus(), status);

        // Actualizar estado y campos relacionados
        claim.setStatus(status);
        if (comments != null) claim.setHandlerComments(comments);

        // Para estados específicos, actualizar campos adicionales
        switch (status) {
            case APPROVED:
            case PARTIALLY_APPROVED:
                if (approvedAmount != null) {
                    claim.setApprovedAmount(approvedAmount);
                    claim.setApprovalDate(LocalDateTime.now());
                } else {
                    throw new BusinessValidationException("Se requiere un monto aprobado para este estado");
                }
                break;
            case DENIED:
                if (denialReason != null) {
                    claim.setDenialReason(denialReason);
                } else {
                    throw new BusinessValidationException("Se requiere un motivo de rechazo para este estado");
                }
                break;
            case ASSESSED:
                claim.setAssessmentDate(LocalDateTime.now());
                break;
            case PAID:
            case PARTIALLY_PAID:
                // Actualizar fecha de liquidación si es la primera vez
                if (claim.getSettlementDate() == null) {
                    claim.setSettlementDate(LocalDateTime.now());
                }
                break;
            default:
                // No se requieren actualizaciones adicionales para otros estados
                break;
        }

        claim.setUpdatedBy(getCurrentUsername());

        // Guardar la reclamación actualizada
        claim = claimRepository.save(claim);

        // Crear registro de historial de estado
        ClaimStatusHistory statusHistory = new ClaimStatusHistory();
        statusHistory.setClaim(claim);
        statusHistory.setPreviousStatus(oldStatus);
        statusHistory.setNewStatus(status);
        statusHistory.setChangeReason(comments);
        statusHistory.setCreatedBy(getCurrentUsername());
        statusHistoryRepository.save(statusHistory);

        // Publicar evento de cambio de estado
        eventProducer.publishClaimStatusChanged(claim, oldStatus);

        log.info("Estado de reclamación actualizado con éxito. ID: {}", id);

        return mapper.toDto(claim);
    }

    @Override
    @Transactional
    public ClaimDto updateClaimStatusByNumber(String claimNumber, Claim.ClaimStatus status, String comments, BigDecimal approvedAmount, String denialReason) {
        log.info("Actualizando estado a {} para reclamación número: {}", status, claimNumber);

        return claimRepository.findByClaimNumber(claimNumber)
                .map(claim -> updateClaimStatus(claim.getId(), status, comments, approvedAmount, denialReason))
                .orElseThrow(() -> new ClaimNotFoundException("Reclamación no encontrada con número: " + claimNumber));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClaimDto> getClaimsByStatus(Claim.ClaimStatus status) {
        log.debug("Obteniendo reclamaciones con estado: {}", status);
        return claimRepository.findByStatus(status).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClaimDto> getClaimsByIncidentDateRange(LocalDate startDate, LocalDate endDate) {
        log.debug("Obteniendo reclamaciones con fecha de incidente entre {} y {}", startDate, endDate);
        return claimRepository.findByIncidentDateBetween(startDate, endDate).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getClaimsDashboardStatistics() {
        log.info("Generando estadísticas de reclamaciones para dashboard");

        Map<String, Object> statistics = new HashMap<>();

        // Recuento por estado
        Map<Claim.ClaimStatus, Long> countByStatus = new HashMap<>();
        for (Claim.ClaimStatus status : Claim.ClaimStatus.values()) {
            long count = claimRepository.findByStatus(status).size();
            countByStatus.put(status, count);
        }
        statistics.put("countByStatus", countByStatus);

        // Recuento por tipo
        Map<Claim.ClaimType, Long> countByType = new HashMap<>();
        for (Claim.ClaimType type : Claim.ClaimType.values()) {
            long count = claimRepository.findByClaimType(type).size();
            countByType.put(type, count);
        }
        statistics.put("countByType", countByType);

        // Reclamaciones recientes (últimos 30 días)
        LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);
        List<Claim> recentClaims = claimRepository.findByIncidentDateBetween(thirtyDaysAgo, LocalDate.now());
        statistics.put("recentClaimsCount", recentClaims.size());

        // Monto total estimado
        BigDecimal totalEstimatedAmount = recentClaims.stream()
                .filter(c -> c.getEstimatedAmount() != null)
                .map(Claim::getEstimatedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        statistics.put("totalEstimatedAmount", totalEstimatedAmount);

        // Monto total aprobado
        BigDecimal totalApprovedAmount = recentClaims.stream()
                .filter(c -> c.getApprovedAmount() != null)
                .map(Claim::getApprovedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        statistics.put("totalApprovedAmount", totalApprovedAmount);

        // Reclamaciones pendientes
        List<Claim> pendingClaims = claimRepository.findByStatus(Claim.ClaimStatus.UNDER_REVIEW);
        pendingClaims.addAll(claimRepository.findByStatus(Claim.ClaimStatus.INFORMATION_REQUESTED));
        pendingClaims.addAll(claimRepository.findByStatus(Claim.ClaimStatus.SUBMITTED));
        statistics.put("pendingClaimsCount", pendingClaims.size());

        return statistics;
    }

    @Override
    public CompletableFuture<Map<String, Object>> getClaimsAdvancedAnalyticsAsync() {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, Object> analytics = new HashMap<>();

            try {
                // Tiempo medio de resolución (días)
                Double avgResolutionTime = statusHistoryRepository.getAverageTransitionTime(
                        Claim.ClaimStatus.SUBMITTED, Claim.ClaimStatus.CLOSED);
                analytics.put("avgResolutionTimeDays", avgResolutionTime != null ? avgResolutionTime : 0);

                // Tasa de aprobación
                List<Claim> allProcessedClaims = new ArrayList<>();
                allProcessedClaims.addAll(claimRepository.findByStatus(Claim.ClaimStatus.APPROVED));
                allProcessedClaims.addAll(claimRepository.findByStatus(Claim.ClaimStatus.PARTIALLY_APPROVED));
                allProcessedClaims.addAll(claimRepository.findByStatus(Claim.ClaimStatus.DENIED));

                long approvedCount = allProcessedClaims.stream()
                        .filter(c -> c.getStatus() == Claim.ClaimStatus.APPROVED
                                || c.getStatus() == Claim.ClaimStatus.PARTIALLY_APPROVED)
                        .count();

                double approvalRate = allProcessedClaims.isEmpty() ? 0 :
                        (double) approvedCount / allProcessedClaims.size() * 100;
                analytics.put("approvalRate", approvalRate);

                // Tendencia mensual (últimos 6 meses)
                Map<String, Long> monthlyTrend = new HashMap<>();
                LocalDate sixMonthsAgo = LocalDate.now().minusMonths(6);
                List<Claim> recentClaims = claimRepository.findByIncidentDateBetween(sixMonthsAgo, LocalDate.now());

                Map<String, List<Claim>> claimsByMonth = recentClaims.stream()
                        .collect(Collectors.groupingBy(
                                claim -> claim.getIncidentDate().format(DateTimeFormatter.ofPattern("yyyy-MM"))));

                for (Map.Entry<String, List<Claim>> entry : claimsByMonth.entrySet()) {
                    monthlyTrend.put(entry.getKey(), (long) entry.getValue().size());
                }
                analytics.put("monthlyTrend", monthlyTrend);

                // Porcentaje de reclamaciones por tipo de póliza
                Map<String, Long> claimsByPolicyType = new HashMap<>();
                for (Claim claim : recentClaims) {
                    String policyType = "UNKNOWN";
                    try {
                        Map<String, Object> policy = policyClient.getPolicyByNumber(claim.getPolicyNumber());
                        policyType = (String) policy.get("policyType");
                    } catch (Exception e) {
                        // Ignorar error y continuar
                    }

                    claimsByPolicyType.put(policyType,
                            claimsByPolicyType.getOrDefault(policyType, 0L) + 1);
                }
                analytics.put("claimsByPolicyType", claimsByPolicyType);

                // Reclamaciones de alto valor
                BigDecimal highValueThreshold = new BigDecimal("10000.00");
                long highValueCount = recentClaims.stream()
                        .filter(c -> c.getEstimatedAmount() != null && c.getEstimatedAmount().compareTo(highValueThreshold) > 0)
                        .count();
                analytics.put("highValueClaimsCount", highValueCount);
                analytics.put("highValuePercentage", recentClaims.isEmpty() ? 0 :
                        (double) highValueCount / recentClaims.size() * 100);

                // Tiempo medio entre etapas clave
                Map<String, Double> avgTimesBetweenStages = new HashMap<>();

                Double timeToAssess = statusHistoryRepository.getAverageTransitionTime(
                        Claim.ClaimStatus.SUBMITTED, Claim.ClaimStatus.ASSESSED);
                avgTimesBetweenStages.put("submitted_to_assessed", timeToAssess != null ? timeToAssess : 0);

                Double timeToApprove = statusHistoryRepository.getAverageTransitionTime(
                        Claim.ClaimStatus.ASSESSED, Claim.ClaimStatus.APPROVED);
                avgTimesBetweenStages.put("assessed_to_approved", timeToApprove != null ? timeToApprove : 0);

                Double timeToPay = statusHistoryRepository.getAverageTransitionTime(
                        Claim.ClaimStatus.APPROVED, Claim.ClaimStatus.PAID);
                avgTimesBetweenStages.put("approved_to_paid", timeToPay != null ? timeToPay : 0);

                analytics.put("avgTimesBetweenStages", avgTimesBetweenStages);

            } catch (Exception e) {
                log.error("Error al generar análisis avanzado", e);
                analytics.put("error", "Error al generar análisis: " + e.getMessage());
            }

            return analytics;
        });
    }

    @Override
    @Transactional
    public ClaimItemDto addClaimItem(Long claimId, ClaimItemDto itemDto) {
        log.info("Añadiendo ítem a reclamación ID: {}", claimId);

        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new ClaimNotFoundException("Reclamación no encontrada con ID: " + claimId));

        // Validar que la reclamación no esté cerrada o cancelada
        if (claim.getStatus() == Claim.ClaimStatus.CLOSED || claim.getStatus() == Claim.ClaimStatus.CANCELLED) {
            throw new BusinessValidationException("No se pueden añadir ítems a una reclamación " + claim.getStatus());
        }

        ClaimItem item = mapper.toEntity(itemDto);
        item.setClaim(claim);
        item.setCreatedBy(getCurrentUsername());
        item.setUpdatedBy(getCurrentUsername());

        item = claimItemRepository.save(item);

        // Recalcular el monto estimado de la reclamación
        updateClaimEstimatedAmount(claim);

        // Publicar evento
        eventProducer.publishClaimItemAdded(claim, item);

        log.info("Ítem añadido con éxito a reclamación ID: {}", claimId);

        return mapper.toDto(item);
    }

    @Override
    @Transactional
    public ClaimItemDto addClaimItemByClaimNumber(String claimNumber, ClaimItemDto itemDto) {
        log.info("Añadiendo ítem a reclamación número: {}", claimNumber);

        return claimRepository.findByClaimNumber(claimNumber)
                .map(claim -> addClaimItem(claim.getId(), itemDto))
                .orElseThrow(() -> new ClaimNotFoundException("Reclamación no encontrada con número: " + claimNumber));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClaimItemDto> getClaimItems(Long claimId) {
        log.debug("Obteniendo ítems para reclamación ID: {}", claimId);
        return claimItemRepository.findByClaimId(claimId).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClaimItemDto> getClaimItemsByClaimNumber(String claimNumber) {
        log.debug("Obteniendo ítems para reclamación número: {}", claimNumber);

        return claimRepository.findByClaimNumber(claimNumber)
                .map(claim -> getClaimItems(claim.getId()))
                .orElseThrow(() -> new ClaimNotFoundException("Reclamación no encontrada con número: " + claimNumber));
    }

    @Override
    @Transactional
    public ClaimItemDto updateClaimItem(String claimNumber, Long itemId, ClaimItemDto itemDto) {
        log.info("Actualizando ítem ID {} de reclamación número: {}", itemId, claimNumber);

        Claim claim = claimRepository.findByClaimNumber(claimNumber)
                .orElseThrow(() -> new ClaimNotFoundException("Reclamación no encontrada con número: " + claimNumber));

        ClaimItem item = claimItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Ítem no encontrado con ID: " + itemId));

        // Validar que el ítem pertenece a la reclamación
        if (!item.getClaim().getId().equals(claim.getId())) {
            throw new BusinessValidationException("El ítem no pertenece a la reclamación especificada");
        }

        // Validar que la reclamación no esté cerrada o cancelada
        if (claim.getStatus() == Claim.ClaimStatus.CLOSED || claim.getStatus() == Claim.ClaimStatus.CANCELLED) {
            throw new BusinessValidationException("No se pueden actualizar ítems de una reclamación " + claim.getStatus());
        }

        // Actualizar campos
        if (itemDto.getDescription() != null) item.setDescription(itemDto.getDescription());
        if (itemDto.getCategory() != null) item.setCategory(itemDto.getCategory());
        if (itemDto.getClaimedAmount() != null) item.setClaimedAmount(itemDto.getClaimedAmount());
        if (itemDto.getApprovedAmount() != null) item.setApprovedAmount(itemDto.getApprovedAmount());
        if (itemDto.getRejectionReason() != null) item.setRejectionReason(itemDto.getRejectionReason());
        item.setCovered(itemDto.isCovered());
        if (itemDto.getEvidenceDocumentId() != null) item.setEvidenceDocumentId(itemDto.getEvidenceDocumentId());
        if (itemDto.getAdditionalDetails() != null) item.setAdditionalDetails(itemDto.getAdditionalDetails());

        item.setUpdatedBy(getCurrentUsername());

        item = claimItemRepository.save(item);

        // Recalcular el monto estimado y aprobado de la reclamación
        updateClaimEstimatedAmount(claim);

        log.info("Ítem actualizado con éxito. ID: {}", itemId);

        return mapper.toDto(item);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClaimSummaryDto> getClaimsSummary(Claim.ClaimStatus status, LocalDate from, LocalDate to) {
        log.info("Obteniendo resumen de reclamaciones");

        List<Claim> claims;

        if (status != null && from != null && to != null) {
            claims = claimRepository.findByMultipleParameters(null, null, null, status, null, from, to);
        } else if (status != null) {
            claims = claimRepository.findByStatus(status);
        } else if (from != null && to != null) {
            claims = claimRepository.findByIncidentDateBetween(from, to);
        } else {
            claims = claimRepository.findAll();
        }

        return claims.stream()
                .map(this::mapToSummary)
                .collect(Collectors.toList());
    }

    @Override
    public String generateClaimNumber(Claim.ClaimType claimType) {
        // Formato: [TIPO]-YYYYMMDD-XXXX donde XXXX es un número aleatorio
        String typeCode = claimType != null ? claimType.name().substring(0, 3) : "CLM";
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomPart = String.format("%04d", new Random().nextInt(10000));
        return typeCode + "-" + datePart + "-" + randomPart;
    }

    // Métodos auxiliares

    private Long resolveCustomerId(ClaimDto claimDto) {
        // Si ya se proporcionó el ID del cliente, usarlo
        if (claimDto.getCustomerId() != null) {
            return claimDto.getCustomerId();
        }

        // Intentar resolver por número de cliente
        if (claimDto.getCustomerNumber() != null && !claimDto.getCustomerNumber().isEmpty()) {
            try {
                Map<String, Object> customer = customerClient.getCustomerByNumber(claimDto.getCustomerNumber());
                Long customerId = ((Number) customer.get("id")).longValue();
                // Guardar también el número de cliente para referencia
                if (claimDto.getCustomerNumber() == null) {
                    claimDto.setCustomerNumber((String) customer.get("customerNumber"));
                }
                return customerId;
            } catch (Exception e) {
                log.error("Error al resolver cliente por número: {}", claimDto.getCustomerNumber(), e);
            }
        }

        // Intentar resolver por email
        if (claimDto.getCustomerEmail() != null && !claimDto.getCustomerEmail().isEmpty()) {
            try {
                Map<String, Object> customer = customerClient.getCustomerByEmail(claimDto.getCustomerEmail());
                Long customerId = ((Number) customer.get("id")).longValue();
                // Guardar también el número de cliente para referencia
                claimDto.setCustomerNumber((String) customer.get("customerNumber"));
                return customerId;
            } catch (Exception e) {
                log.error("Error al resolver cliente por email: {}", claimDto.getCustomerEmail(), e);
            }
        }

        // Intentar resolver por identificación
        if (claimDto.getIdentificationNumber() != null && claimDto.getIdentificationType() != null) {
            try {
                Map<String, Object> customer = customerClient.getCustomerByIdentification(
                        claimDto.getIdentificationNumber(), claimDto.getIdentificationType());
                Long customerId = ((Number) customer.get("id")).longValue();
                // Guardar también el número de cliente para referencia
                claimDto.setCustomerNumber((String) customer.get("customerNumber"));
                return customerId;
            } catch (Exception e) {
                log.error("Error al resolver cliente por identificación: {}/{}",
                        claimDto.getIdentificationNumber(), claimDto.getIdentificationType(), e);
            }
        }

        // Si ninguno de los métodos tiene éxito, lanzar excepción
        throw new BusinessValidationException("No se pudo identificar al cliente. Por favor, proporcione información válida.");
    }

    private Long resolvePolicyId(ClaimDto claimDto) {
        // Si ya se proporcionó el ID de la póliza, usarlo
        if (claimDto.getPolicyId() != null) {
            return claimDto.getPolicyId();
        }

        // Intentar resolver por número de póliza
        if (claimDto.getPolicyNumber() != null && !claimDto.getPolicyNumber().isEmpty()) {
            try {
                Map<String, Object> policy = policyClient.getPolicyByNumber(claimDto.getPolicyNumber());
                Long policyId = ((Number) policy.get("id")).longValue();
                return policyId;
            } catch (Exception e) {
                log.error("Error al resolver póliza por número: {}", claimDto.getPolicyNumber(), e);
            }
        }

        // Intentar resolver por cliente
        Long customerId = claimDto.getCustomerId();
        if (customerId != null) {
            try {
                List<Map<String, Object>> policies = policyClient.getPoliciesByCustomerId(customerId);
                if (!policies.isEmpty()) {
                    Map<String, Object> policy = policies.get(0); // Usar la primera póliza encontrada
                    Long policyId = ((Number) policy.get("id")).longValue();
                    claimDto.setPolicyNumber((String) policy.get("policyNumber"));
                    return policyId;
                }
            } catch (Exception e) {
                log.error("Error al obtener pólizas para cliente ID: {}", customerId, e);
            }
        }

        // Intentar resolver por número de cliente
        String customerNumber = claimDto.getCustomerNumber();
        if (customerNumber != null && !customerNumber.isEmpty()) {
            try {
                List<Map<String, Object>> policies = policyClient.getPoliciesByCustomerNumber(customerNumber);
                if (!policies.isEmpty()) {
                    Map<String, Object> policy = policies.get(0); // Usar la primera póliza encontrada
                    Long policyId = ((Number) policy.get("id")).longValue();
                    claimDto.setPolicyNumber((String) policy.get("policyNumber"));
                    return policyId;
                }
            } catch (Exception e) {
                log.error("Error al obtener pólizas para cliente número: {}", customerNumber, e);
            }
        }

        // Si no se encontró ninguna póliza, la reclamación puede crearse sin referencia a póliza
        return null;
    }

    private void validateStatusTransition(Claim.ClaimStatus currentStatus, Claim.ClaimStatus newStatus) {
        // Reglas para transiciones inválidas (simplificadas para este ejemplo)
        if (currentStatus == Claim.ClaimStatus.CLOSED && newStatus != Claim.ClaimStatus.REOPENED) {
            throw new BusinessValidationException("Una reclamación CLOSED solo puede cambiar a REOPENED");
        }

        if (currentStatus == Claim.ClaimStatus.CANCELLED) {
            throw new BusinessValidationException("Una reclamación CANCELLED no puede cambiar de estado");
        }

        // Reglas específicas según flujo de negocio (ejemplo)
        boolean validTransition = switch (currentStatus) {
            case SUBMITTED -> newStatus == Claim.ClaimStatus.UNDER_REVIEW
                    || newStatus == Claim.ClaimStatus.INFORMATION_REQUESTED
                    || newStatus == Claim.ClaimStatus.CANCELLED;
            case UNDER_REVIEW -> newStatus == Claim.ClaimStatus.INFORMATION_REQUESTED
                    || newStatus == Claim.ClaimStatus.ASSESSED
                    || newStatus == Claim.ClaimStatus.CANCELLED;
            case INFORMATION_REQUESTED -> newStatus == Claim.ClaimStatus.UNDER_REVIEW
                    || newStatus == Claim.ClaimStatus.CANCELLED;
            case ASSESSED -> newStatus == Claim.ClaimStatus.APPROVED
                    || newStatus == Claim.ClaimStatus.PARTIALLY_APPROVED
                    || newStatus == Claim.ClaimStatus.DENIED
                    || newStatus == Claim.ClaimStatus.INFORMATION_REQUESTED;
            case APPROVED, PARTIALLY_APPROVED -> newStatus == Claim.ClaimStatus.PAYMENT_IN_PROCESS
                    || newStatus == Claim.ClaimStatus.PAID
                    || newStatus == Claim.ClaimStatus.PARTIALLY_PAID;
            case PAYMENT_IN_PROCESS -> newStatus == Claim.ClaimStatus.PAID
                    || newStatus == Claim.ClaimStatus.PARTIALLY_PAID;
            case PAID, PARTIALLY_PAID -> newStatus == Claim.ClaimStatus.CLOSED;
            case DENIED -> newStatus == Claim.ClaimStatus.CLOSED
                    || newStatus == Claim.ClaimStatus.REOPENED;
            case REOPENED -> newStatus == Claim.ClaimStatus.UNDER_REVIEW
                    || newStatus == Claim.ClaimStatus.INFORMATION_REQUESTED;
            case CANCELLED, CLOSED -> false; // Manejados específicamente arriba
            default -> false;
        };

        if (!validTransition) {
            throw new BusinessValidationException("Transición de estado inválida: " + currentStatus + " a " + newStatus);
        }
    }

    private void updateClaimEstimatedAmount(Claim claim) {
        // Calcular sumatorio de montos reclamados
        BigDecimal totalClaimed = claimItemRepository.sumClaimedAmountsByClaimId(claim.getId());
        if (totalClaimed != null) {
            claim.setEstimatedAmount(totalClaimed);
        }

        // Calcular sumatorio de montos aprobados
        BigDecimal totalApproved = claimItemRepository.sumApprovedAmountsByClaimId(claim.getId());
        if (totalApproved != null) {
            claim.setApprovedAmount(totalApproved);
        }

        claimRepository.save(claim);
    }

    private String getCurrentUsername() {
        try {
            return SecurityContextHolder.getContext().getAuthentication().getName();
        } catch (Exception e) {
            return "system";
        }
    }

    private ClaimSummaryDto mapToSummary(Claim claim) {
        ClaimSummaryDto summary = new ClaimSummaryDto();
        summary.setId(claim.getId());
        summary.setClaimNumber(claim.getClaimNumber());
        summary.setPolicyNumber(claim.getPolicyNumber());
        summary.setCustomerNumber(claim.getCustomerNumber());

        // Intentar obtener el nombre del cliente
        try {
            Map<String, Object> customer = customerClient.getCustomerByNumber(claim.getCustomerNumber());
            String firstName = (String) customer.get("firstName");
            String lastName = (String) customer.get("lastName");
            summary.setCustomerName(firstName + " " + lastName);
        } catch (Exception e) {
            summary.setCustomerName("Desconocido");
        }

        summary.setIncidentDate(claim.getIncidentDate());
        summary.setStatus(claim.getStatus());
        summary.setClaimType(claim.getClaimType());
        summary.setEstimatedAmount(claim.getEstimatedAmount());
        summary.setApprovedAmount(claim.getApprovedAmount());
        summary.setSubmissionDate(claim.getSubmissionDate());

        // Calcular días abiertos
        if (claim.getSubmissionDate() != null) {
            long daysOpen = ChronoUnit.DAYS.between(claim.getSubmissionDate().toLocalDate(), LocalDate.now());
            summary.setDaysOpen(daysOpen);
        }

        return summary;
    }
}
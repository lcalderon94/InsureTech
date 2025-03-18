package com.insurtech.policy.service.impl;

import com.insurtech.policy.client.CustomerClient;
import com.insurtech.policy.event.producer.PolicyEventProducer;
import com.insurtech.policy.exception.BusinessValidationException;
import com.insurtech.policy.exception.PolicyNotFoundException;
import com.insurtech.policy.model.dto.PolicyDto;
import com.insurtech.policy.model.dto.PolicyNoteDto;
import com.insurtech.policy.model.entity.Policy;
import com.insurtech.policy.model.entity.PolicyEvent;
import com.insurtech.policy.model.entity.PolicyNote;
import com.insurtech.policy.repository.PolicyRepository;
import com.insurtech.policy.repository.PolicyNoteRepository;
import com.insurtech.policy.service.PolicyService;
import com.insurtech.policy.service.PolicyVersioningService;
import com.insurtech.policy.service.async.AsyncPolicyProcessor;
import com.insurtech.policy.service.async.ConcurrentPolicyValidator;
import com.insurtech.policy.util.EntityDtoMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PolicyServiceImpl implements PolicyService {

    private static final Logger log = LoggerFactory.getLogger(PolicyServiceImpl.class);

    private final PolicyRepository policyRepository;
    private final PolicyNoteRepository policyNoteRepository;
    private final EntityDtoMapper mapper;
    private final PolicyEventProducer eventProducer;
    private final PolicyVersioningService policyVersioningService;
    private final CustomerClient customerClient;
    private final AsyncPolicyProcessor asyncPolicyProcessor;
    private final ConcurrentPolicyValidator concurrentPolicyValidator;

    @Override
    @Transactional
    public PolicyDto createPolicy(PolicyDto policyDto) {
        log.info("Creando póliza para cliente: {}", policyDto.getCustomerId());

        // Validar cliente
        validateCustomer(policyDto.getCustomerId());

        // Generar número de póliza si no se proporcionó
        if (policyDto.getPolicyNumber() == null) {
            policyDto.setPolicyNumber(generatePolicyNumber(policyDto.getPolicyType()));
        }

        // Por defecto, una nueva póliza está en estado DRAFT si no se especifica
        if (policyDto.getStatus() == null) {
            policyDto.setStatus(Policy.PolicyStatus.DRAFT);
        }

        // Convertir DTO a entidad
        Policy policy = mapper.toEntity(policyDto);
        policy.setCreatedBy(getCurrentUsername());
        policy.setUpdatedBy(getCurrentUsername());

        // Guardar la póliza
        policy = policyRepository.save(policy);

        // Crear versión inicial
        policyVersioningService.createPolicyVersion(policy.getId(), "Creación inicial de póliza");

        // Publicar evento
        eventProducer.publishPolicyCreated(policy);

        log.info("Póliza creada con éxito. ID: {}, Número: {}", policy.getId(), policy.getPolicyNumber());

        return mapper.toDto(policy);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PolicyDto> getPolicyById(Long id) {
        log.debug("Obteniendo póliza por ID: {}", id);
        return policyRepository.findById(id)
                .map(mapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PolicyDto> getPolicyByNumber(String policyNumber) {
        log.debug("Obteniendo póliza por número: {}", policyNumber);
        return policyRepository.findByPolicyNumber(policyNumber)
                .map(mapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PolicyDto> searchPolicies(String searchTerm, Pageable pageable) {
        log.debug("Buscando pólizas con término: {}", searchTerm);
        return policyRepository.search(searchTerm, pageable)
                .map(mapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PolicyDto> getPoliciesByCustomerId(Long customerId) {
        log.debug("Obteniendo pólizas para el cliente: {}", customerId);
        return policyRepository.findByCustomerId(customerId).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PolicyDto> getPoliciesByCustomerId(Long customerId, Pageable pageable) {
        log.debug("Obteniendo pólizas paginadas para el cliente: {}", customerId);
        return policyRepository.findByCustomerId(customerId, pageable)
                .map(mapper::toDto);
    }

    @Override
    @Transactional
    public PolicyDto updatePolicy(Long id, PolicyDto policyDto) {
        log.info("Actualizando póliza con ID: {}", id);

        Policy policy = policyRepository.findById(id)
                .orElseThrow(() -> new PolicyNotFoundException("Póliza no encontrada con ID: " + id));

        // Backup del estado anterior para evento de cambio de estado
        Policy.PolicyStatus oldStatus = policy.getStatus();

        // Validar que no se pueda actualizar una póliza cancelada o expirada
        if (policy.getStatus() == Policy.PolicyStatus.CANCELLED || policy.getStatus() == Policy.PolicyStatus.EXPIRED) {
            throw new BusinessValidationException("No se puede actualizar una póliza " + policy.getStatus());
        }

        // Actualizar campos básicos
        if (policyDto.getStartDate() != null) policy.setStartDate(policyDto.getStartDate());
        if (policyDto.getEndDate() != null) policy.setEndDate(policyDto.getEndDate());
        if (policyDto.getPremium() != null) policy.setPremium(policyDto.getPremium());
        if (policyDto.getSumInsured() != null) policy.setSumInsured(policyDto.getSumInsured());
        if (policyDto.getPaymentFrequency() != null) policy.setPaymentFrequency(policyDto.getPaymentFrequency());
        if (policyDto.getPaymentMethod() != null) policy.setPaymentMethod(policyDto.getPaymentMethod());
        if (policyDto.getDescription() != null) policy.setDescription(policyDto.getDescription());
        if (policyDto.getStatus() != null) policy.setStatus(policyDto.getStatus());

        policy.setUpdatedBy(getCurrentUsername());

        // Guardar la póliza actualizada
        policy = policyRepository.save(policy);

        // Crear una nueva versión
        policyVersioningService.createPolicyVersion(policy.getId(), "Actualización de póliza");

        // Publicar evento de póliza actualizada
        eventProducer.publishPolicyUpdated(policy);

        // Publicar evento de cambio de estado si aplica
        if (oldStatus != policy.getStatus()) {
            eventProducer.publishPolicyStatusChanged(policy, oldStatus);
        }

        log.info("Póliza actualizada con éxito. ID: {}", id);

        return mapper.toDto(policy);
    }

    @Override
    @Transactional
    public void deletePolicy(Long id) {
        log.info("Eliminando póliza con ID: {}", id);

        Policy policy = policyRepository.findById(id)
                .orElseThrow(() -> new PolicyNotFoundException("Póliza no encontrada con ID: " + id));

        // Solo se permite eliminar pólizas en estado DRAFT
        if (policy.getStatus() != Policy.PolicyStatus.DRAFT) {
            throw new BusinessValidationException("Solo se pueden eliminar pólizas en estado DRAFT");
        }

        policyRepository.delete(policy);

        log.info("Póliza eliminada con éxito. ID: {}", id);
    }

    @Override
    @Transactional
    public PolicyDto updatePolicyStatus(Long id, Policy.PolicyStatus status, String reason) {
        log.info("Actualizando estado a {} para póliza ID: {}", status, id);

        Policy policy = policyRepository.findById(id)
                .orElseThrow(() -> new PolicyNotFoundException("Póliza no encontrada con ID: " + id));

        Policy.PolicyStatus oldStatus = policy.getStatus();

        // Validar transición de estado
        validateStatusTransition(policy.getStatus(), status);

        policy.setStatus(status);
        policy.setUpdatedBy(getCurrentUsername());

        // Si se activa la póliza, establecer la fecha de emisión
        if (status == Policy.PolicyStatus.ACTIVE && policy.getIssueDate() == null) {
            policy.setIssueDate(LocalDate.now());
        }

        // Guardar la póliza
        policy = policyRepository.save(policy);

        // Agregar nota sobre el cambio de estado
        PolicyNote note = new PolicyNote();
        note.setPolicy(policy);
        note.setNoteType(PolicyNote.NoteType.GENERAL);
        note.setTitle("Cambio de estado");
        note.setContent("Estado cambiado de " + oldStatus + " a " + status + ". Razón: " + reason);
        note.setSystemGenerated(true);
        note.setCreatedBy(getCurrentUsername());
        policyNoteRepository.save(note);

        // Crear una nueva versión
        policyVersioningService.createPolicyVersion(policy.getId(), "Cambio de estado: " + oldStatus + " -> " + status);

        // Publicar evento de cambio de estado
        eventProducer.publishPolicyStatusChanged(policy, oldStatus);

        log.info("Estado de póliza actualizado con éxito. ID: {}", id);

        return mapper.toDto(policy);
    }

    @Override
    @Transactional
    public PolicyNoteDto addPolicyNote(Long policyId, PolicyNoteDto noteDto) {
        log.info("Añadiendo nota a la póliza ID: {}", policyId);

        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new PolicyNotFoundException("Póliza no encontrada con ID: " + policyId));

        PolicyNote note = new PolicyNote();
        note.setPolicy(policy);
        note.setNoteType(noteDto.getNoteType());
        note.setTitle(noteDto.getTitle());
        note.setContent(noteDto.getContent());
        note.setImportant(noteDto.isImportant());
        note.setSystemGenerated(false);
        note.setCreatedBy(getCurrentUsername());
        note.setUpdatedBy(getCurrentUsername());

        note = policyNoteRepository.save(note);

        log.info("Nota añadida con éxito a póliza ID: {}", policyId);

        return mapper.toDto(note);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PolicyDto> getPoliciesExpiringBetween(LocalDate startDate, LocalDate endDate) {
        log.debug("Obteniendo pólizas que expiran entre {} y {}", startDate, endDate);
        return policyRepository.findPoliciesExpiringBetween(startDate, endDate).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public CompletableFuture<Double> calculatePremiumAsync(PolicyDto policyDto) {
        log.info("Calculando prima de forma asíncrona para póliza");
        return asyncPolicyProcessor.calculatePremium(policyDto);
    }

    @Override
    public CompletableFuture<Map<String, Object>> calculateCustomerPolicyStatisticsAsync(Long customerId) {
        log.info("Calculando estadísticas de pólizas para cliente ID: {}", customerId);
        return asyncPolicyProcessor.calculateCustomerPolicyStatistics(customerId);
    }

    @Override
    public String generatePolicyNumber(Policy.PolicyType policyType) {
        // Formato: [TIPO]-YYYYMMDD-XXXX donde XXXX es un número aleatorio
        String typeCode = policyType.name().substring(0, 3);
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomPart = String.format("%04d", new Random().nextInt(10000));
        return typeCode + "-" + datePart + "-" + randomPart;
    }

    @Override
    public CompletableFuture<Boolean> validatePolicyAsync(PolicyDto policyDto) {
        log.info("Validando póliza de forma concurrente");
        return concurrentPolicyValidator.validatePolicy(policyDto);
    }

    // Métodos auxiliares

    private void validateCustomer(Long customerId) {
        // Verificar que el cliente existe usando Feign Client
        try {
            customerClient.getCustomerById(customerId);
        } catch (Exception e) {
            log.error("Error validando cliente ID: {}", customerId, e);
            throw new BusinessValidationException("Cliente no encontrado con ID: " + customerId);
        }
    }

    private void validateStatusTransition(Policy.PolicyStatus currentStatus, Policy.PolicyStatus newStatus) {
        // Implementación simplificada de reglas de transición de estado
        // En una implementación real, esto sería más complejo con una matriz de transiciones válidas

        if (currentStatus == Policy.PolicyStatus.CANCELLED || currentStatus == Policy.PolicyStatus.EXPIRED) {
            throw new BusinessValidationException("No se puede cambiar el estado de una póliza " + currentStatus);
        }

        // Ejemplo: Una póliza DRAFT solo puede pasar a QUOTED o CANCELLED
        if (currentStatus == Policy.PolicyStatus.DRAFT &&
                !(newStatus == Policy.PolicyStatus.QUOTED || newStatus == Policy.PolicyStatus.CANCELLED)) {
            throw new BusinessValidationException("No se puede cambiar una póliza DRAFT a " + newStatus);
        }

        // Otros casos específicos
        if (currentStatus == Policy.PolicyStatus.ACTIVE && newStatus == Policy.PolicyStatus.DRAFT) {
            throw new BusinessValidationException("No se puede cambiar una póliza ACTIVE a DRAFT");
        }
    }

    private String getCurrentUsername() {
        try {
            return SecurityContextHolder.getContext().getAuthentication().getName();
        } catch (Exception e) {
            return "system";
        }
    }
}